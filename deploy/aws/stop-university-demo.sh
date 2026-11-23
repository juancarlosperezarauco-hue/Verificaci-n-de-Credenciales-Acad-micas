#!/usr/bin/env bash
set -euo pipefail
[[ "$EUID" -eq 0 ]] || exit 1
root="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$root"
demo_env=/etc/credenciales/university-demo.env
if [[ -f "$demo_env" && ! -L "$demo_env" ]]; then
  docker compose --env-file "$demo_env" -f compose.university-demo-tunnel.yml down
fi
app=(docker compose --env-file /etc/credenciales/private.env -f compose.aws-private.yml)
"${app[@]}" up -d --no-deps --wait --wait-timeout 240 backend
"${app[@]}" restart frontend
"${app[@]}" up -d --no-deps --wait --wait-timeout 120 frontend
echo 'Temporary public demo stopped; private access restored. No data or encryption keys removed.'
