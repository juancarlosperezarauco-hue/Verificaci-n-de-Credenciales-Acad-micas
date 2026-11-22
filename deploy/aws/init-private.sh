#!/usr/bin/env bash
# Generate independent evaluation secrets on the host, never in EC2 user data.
set -euo pipefail
umask 077
secret_dir="${1:-/etc/credenciales}"
admin_email="${2:-superadmin@pruebas.invalid}"
[[ "$admin_email" =~ ^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$ ]] || { echo 'Invalid bootstrap email' >&2; exit 1; }
mkdir -p "$secret_dir"
chmod 700 "$secret_dir"
exec 9>"$secret_dir/.init.lock"
flock -x 9
env_file="$secret_dir/private.env"
# Never silently regenerate encryption keys used by an existing database.
if [[ -e "$env_file" || -L "$env_file" ]]; then
  [[ -f "$env_file" && ! -L "$env_file" ]] || exit 1
  [[ "$(stat -c %a "$env_file")" == 600 ]] || { echo 'Expected private.env mode 600' >&2; exit 1; }
  echo 'Existing evaluation keys preserved.'
  exit 0
fi
[[ ! -e "$secret_dir/bootstrap-admin.txt" && ! -L "$secret_dir/bootstrap-admin.txt" ]] || { echo 'Bootstrap credentials already exist; inspect manually.' >&2; exit 1; }
tmp_file="$(mktemp "$secret_dir/.private.XXXXXX")"
trap 'rm -f "$tmp_file"' EXIT
for key in POSTGRES_PASSWORD MINIO_ROOT_PASSWORD JWT_SECRET AI_SERVICE_TOKEN; do
  printf '%s=%s\n' "$key" "$(openssl rand -hex 32)" >> "$tmp_file"
done
for key in MFA_ENCRYPTION_KEY MAIL_ENCRYPTION_KEY EVIDENCE_ENCRYPTION_KEY SIGNING_ENCRYPTION_KEY; do
  printf '%s=%s\n' "$key" "$(openssl rand -base64 32)" >> "$tmp_file"
done
admin_password="$(openssl rand -hex 24)"
printf 'MINIO_ROOT_USER=credentials-private\nBOOTSTRAP_SUPERADMIN_EMAIL=%s\nBOOTSTRAP_SUPERADMIN_PASSWORD=%s\n' "$admin_email" "$admin_password" >> "$tmp_file"
printf 'email=%s\npassword=%s\n' "$admin_email" "$admin_password" > "$secret_dir/bootstrap-admin.txt"
chmod 600 "$secret_dir/bootstrap-admin.txt" "$tmp_file"
mv "$tmp_file" "$env_file"
echo 'Evaluation keys generated locally. Bootstrap credentials stored in the protected host directory.'
