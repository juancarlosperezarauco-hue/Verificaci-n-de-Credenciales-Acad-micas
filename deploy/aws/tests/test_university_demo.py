"""A public presentation must not publish internal services or inherit their secrets."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import test_private_deploy as fixtures

ROOT = Path(__file__).resolve().parents[3]


class UniversityDemoTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.docker = shutil.which('docker')
        if not cls.docker and os.name == 'nt':
            candidate = Path(os.environ['LOCALAPPDATA']) / 'Programs/DockerDesktop/resources/bin/docker.exe'
            if candidate.exists(): cls.docker = str(candidate)
        if not cls.docker: raise unittest.SkipTest('Docker Compose CLI required')

    def compose(self, *files):
        env = os.environ.copy()
        env.update({key: 'fixture-' + key for key in fixtures.REQUIRED})
        env.update(DEMO_PUBLIC_URL='https://presentation.example.test',
                   DEMO_TUNNEL_IMAGE='cloudflare/cloudflared@sha256:' + 'a' * 64)
        with tempfile.NamedTemporaryFile(mode='w', suffix='.env', delete=False) as f:
            path = f.name
        try:
            args = [self.docker, 'compose', '--env-file', path]
            for file in files: args += ['-f', str(ROOT / file)]
            result = subprocess.run(args + ['config', '--format', 'json'], env=env, text=True, capture_output=True)
            self.assertEqual(result.returncode, 0, result.stderr)
            return json.loads(result.stdout)
        finally: Path(path).unlink()

    def test_public_origin_preserves_private_access_without_publishing_more_ports(self):
        config = self.compose('compose.aws-private.yml', 'compose.university-demo.yml')
        services = config['services']
        self.assertEqual(services['backend']['environment']['APP_FRONTEND_URL'], 'https://presentation.example.test')
        self.assertEqual(services['backend']['environment']['ALLOWED_ORIGIN'], 'https://presentation.example.test,http://localhost:4302')
        exposed = {(name, port['host_ip'], port['published']) for name, service in services.items() for port in service.get('ports', [])}
        self.assertEqual(exposed, {('frontend', '127.0.0.1', '4302'), ('mailpit', '127.0.0.1', '8126')})

    def test_tunnel_has_no_host_ports_secrets_or_host_mounts(self):
        config = self.compose('compose.university-demo-tunnel.yml')
        service = config['services']['tunnel']
        self.assertFalse(service.get('ports'))
        self.assertFalse(service.get('environment'))
        self.assertFalse(service.get('volumes'))
        self.assertTrue(service['read_only'])
        self.assertIn('ALL', service['cap_drop'])
        self.assertTrue(config['networks']['origin']['external'])
        self.assertEqual(config['networks']['origin']['name'], 'credenciales-aws-private_default')

    def test_tunnel_uses_http2_ipv4_and_does_not_restart_with_a_stale_url(self):
        service = self.compose('compose.university-demo-tunnel.yml')['services']['tunnel']
        self.assertEqual(service['restart'], 'no')
        self.assertEqual(service['command'], ['tunnel', '--no-autoupdate', '--protocol', 'http2', '--edge-ip-version', '4', '--url', 'http://frontend:80'])


if __name__ == '__main__': unittest.main()
