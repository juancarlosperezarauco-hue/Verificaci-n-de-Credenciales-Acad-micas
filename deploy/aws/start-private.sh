#!/usr/bin/env bash
set -euo pipefail
[[ "$EUID" -eq 0 ]] || { echo 'Run as root' >&2; exit 1; }
repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
env_file=/etc/credenciales/private.env
[[ -f "$env_file" && ! -L "$env_file" && "$(stat -c %a "$env_file")" == 600 ]] || { echo 'Initialize protected evaluation keys first' >&2; exit 1; }
cd "$repo_root"
compose=(docker compose --env-file "$env_file" -f compose.aws-private.yml)
"${compose[@]}" config --quiet
# Sequential builds keep the 4 GiB evaluation host within a predictable workload.
for service in backend frontend ai-service; do
  "${compose[@]}" build "$service"
done
"${compose[@]}" up -d --wait --wait-timeout 600
curl --fail --silent --output /dev/null http://127.0.0.1:4302/
# Remove the one-time password from running container configuration after bootstrap.
sed -i -E 's/^(BOOTSTRAP_SUPERADMIN_(EMAIL|PASSWORD))=.*/\1=/' "$env_file"
chmod 600 "$env_file"
"${compose[@]}" up -d --wait --wait-timeout 600 backend frontend
curl --fail --silent --output /dev/null http://127.0.0.1:4302/
echo 'Private evaluation ready. Connect port 4302 using AWS Session Manager.'
