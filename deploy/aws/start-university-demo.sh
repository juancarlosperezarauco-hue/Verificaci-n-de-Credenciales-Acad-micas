#!/usr/bin/env bash
set -euo pipefail
[[ "$EUID" -eq 0 ]] || { echo 'Run as root' >&2; exit 1; }
umask 077
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
private_env=/etc/credenciales/private.env
demo_env=/etc/credenciales/university-demo.env
[[ -f "$private_env" && ! -L "$private_env" && "$(stat -c %a "$private_env")" == 600 ]] || exit 1
curl --fail --silent --output /dev/null http://127.0.0.1:4302/
docker pull cloudflare/cloudflared:2026.11.1
image="$(docker image inspect cloudflare/cloudflared:2026.11.1 --format '{{index .RepoDigests 0}}')"
[[ "$image" =~ ^cloudflare/cloudflared@sha256:[0-9a-f]{64}$ ]] || { echo 'Unexpected tunnel image digest' >&2; exit 1; }
if [[ -L "$demo_env" ]]; then echo 'Unexpected metadata symlink' >&2; exit 1; fi
printf 'DEMO_TUNNEL_IMAGE=%s\n' "$image" > "$demo_env"
chmod 600 "$demo_env"
tunnel=(docker compose --env-file "$demo_env" -f compose.university-demo-tunnel.yml)
"${tunnel[@]}" up -d --no-recreate
container="$("${tunnel[@]}" ps -q tunnel)"
[[ -n "$container" ]] || { echo 'Tunnel container not started' >&2; exit 1; }
started="$(docker inspect "$container" --format '{{.State.StartedAt}}')"
url=''
for attempt in $(seq 1 60); do
  url="$(docker logs --since "$started" "$container" 2>&1 | grep -Eo 'https://[a-z0-9-]+\.trycloudflare\.com' | tail -n 1 || true)"
  [[ -n "$url" ]] && break
  [[ "$(docker inspect "$container" --format '{{.State.Running}}')" == true ]] || { echo 'Tunnel stopped before assigning a URL' >&2; exit 1; }
  sleep 1
done
[[ "$url" =~ ^https://[a-z0-9-]+\.trycloudflare\.com$ ]] || { echo 'No valid temporary HTTPS URL' >&2; exit 1; }
printf 'DEMO_PUBLIC_URL=%s\n' "$url" >> "$demo_env"
app=(docker compose --env-file "$private_env" --env-file "$demo_env" -f compose.aws-private.yml -f compose.university-demo.yml)
"${app[@]}" config --quiet
"${app[@]}" build backend
"${app[@]}" up -d --no-deps --wait --wait-timeout 240 backend
# nginx resolves backend when it starts; refresh that resolution after replacement.
"${app[@]}" restart frontend
"${app[@]}" up -d --no-deps --wait --wait-timeout 120 frontend
curl --fail --silent --output /dev/null http://127.0.0.1:4302/
printf 'UNIVERSITY_DEMO_URL=%s\n' "$url"
