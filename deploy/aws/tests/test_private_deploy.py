"""Deployment boundaries and key compatibility; no AWS resources are created."""
import base64
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[3]
AES_KEYS = ('MFA_ENCRYPTION_KEY', 'MAIL_ENCRYPTION_KEY', 'EVIDENCE_ENCRYPTION_KEY', 'SIGNING_ENCRYPTION_KEY')
REQUIRED = ('POSTGRES_PASSWORD', 'MINIO_ROOT_USER', 'MINIO_ROOT_PASSWORD', 'JWT_SECRET', 'AI_SERVICE_TOKEN', *AES_KEYS)


class ComposeBoundaryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        executable = shutil.which('docker')
        if not executable and os.name == 'nt':
            candidate = Path(os.environ['LOCALAPPDATA']) / 'Programs/DockerDesktop/resources/bin/docker.exe'
            if candidate.exists():
                executable = str(candidate)
        if not executable:
            raise unittest.SkipTest('Docker Compose CLI required; engine not required')
        cls.docker = executable

    def compose(self, missing=None):
        env = os.environ.copy()
        # Disposable fixture values; never read the developer's real .env.
        env.update({key: 'fixture-only-' + key for key in REQUIRED})
        for key in ('BOOTSTRAP_SUPERADMIN_EMAIL', 'BOOTSTRAP_SUPERADMIN_PASSWORD'):
            env.pop(key, None)
        if missing:
            env.pop(missing, None)
        with tempfile.NamedTemporaryFile(mode='w', suffix='.env', delete=False) as f:
            env_path = f.name
        try:
            return subprocess.run([self.docker, 'compose', '--env-file', env_path, '-f', str(ROOT / 'compose.aws-private.yml'), 'config', '--format', 'json'], cwd=ROOT, env=env, text=True, capture_output=True)
        finally:
            Path(env_path).unlink()

    def test_only_tunnel_endpoints_are_published(self):
        result = self.compose()
        self.assertEqual(result.returncode, 0, result.stderr)
        services = json.loads(result.stdout)['services']
        exposed = {(name, port['host_ip'], port['published'], port['target']) for name, service in services.items() for port in service.get('ports', [])}
        self.assertEqual(exposed, {('frontend', '127.0.0.1', '4302', 80), ('mailpit', '127.0.0.1', '8126', 8025)})
        self.assertEqual(services['backend']['environment']['ALLOWED_ORIGIN'], 'http://localhost:4302')
        self.assertEqual(services['backend']['environment']['APP_FRONTEND_URL'], 'http://localhost:4302')
        self.assertNotEqual(services['backend']['environment']['SPRING_PROFILES_ACTIVE'], 'production')

    def test_missing_secrets_fail_before_start(self):
        for key in REQUIRED:
            with self.subTest(key=key):
                result = self.compose(missing=key)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn(key, result.stderr)

    def test_private_services_have_limits_and_no_privileged_host_access(self):
        services = json.loads(self.compose().stdout)['services']
        for name, service in services.items():
            with self.subTest(service=name):
                self.assertGreater(int(service['mem_limit']), 0)
                self.assertFalse(service.get('privileged', False))
                self.assertNotEqual(service.get('network_mode'), 'host')
                self.assertFalse(any('docker.sock' in str(volume) for volume in service.get('volumes', [])))
        for name in ('backend', 'ai-service'):
            self.assertTrue(services[name]['read_only'])
            self.assertIn('ALL', services[name]['cap_drop'])


@unittest.skipIf(os.name == 'nt', 'POSIX permissions require Linux; exercised in CI')
class SecretLifecycleTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.directory = Path(self.tmp.name) / 'secrets'

    def tearDown(self):
        self.tmp.cleanup()

    def init(self, email='superadmin@pruebas.invalid'):
        return subprocess.run(['bash', str(ROOT / 'deploy/aws/init-private.sh'), str(self.directory), email], text=True, capture_output=True)

    def test_keys_are_independent_compatible_and_not_logged(self):
        result = self.init()
        self.assertEqual(result.returncode, 0, result.stderr)
        env_file = self.directory / 'private.env'
        values = dict(line.split('=', 1) for line in env_file.read_text().splitlines())
        self.assertEqual(len({values[key] for key in REQUIRED if key != 'MINIO_ROOT_USER'}), len(REQUIRED) - 1)
        for key in AES_KEYS:
            self.assertEqual(len(base64.b64decode(values[key], validate=True)), 32)
        self.assertGreaterEqual(len(values['JWT_SECRET'].encode()), 32)
        for path in (env_file, self.directory / 'bootstrap-admin.txt'):
            self.assertEqual(path.stat().st_mode & 0o777, 0o600)
        for key in REQUIRED:
            self.assertNotIn(values[key], result.stdout + result.stderr)
        first = env_file.read_bytes()
        self.assertEqual(self.init().returncode, 0)
        self.assertEqual(env_file.read_bytes(), first)

    def test_insecure_existing_file_is_rejected_and_preserved(self):
        self.assertEqual(self.init().returncode, 0)
        env_file = self.directory / 'private.env'
        first = env_file.read_bytes()
        env_file.chmod(0o644)
        self.assertNotEqual(self.init().returncode, 0)
        self.assertEqual(env_file.read_bytes(), first)

    def test_symlink_is_rejected_without_overwriting_target(self):
        self.directory.mkdir()
        target = Path(self.tmp.name) / 'other-secret'
        target.write_text('preserve')
        (self.directory / 'private.env').symlink_to(target)
        self.assertNotEqual(self.init().returncode, 0)
        self.assertEqual(target.read_text(), 'preserve')

    def test_invalid_email_is_rejected_before_writing_secrets(self):
        self.assertNotEqual(self.init('bad\nSMTP_PASSWORD=injected').returncode, 0)
        self.assertFalse(self.directory.exists())


if __name__ == '__main__':
    unittest.main()
