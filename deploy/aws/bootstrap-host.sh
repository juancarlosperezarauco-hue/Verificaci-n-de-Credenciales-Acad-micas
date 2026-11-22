#!/usr/bin/env bash
# Ubuntu 24.04; no credentials, inbound listeners or Docker TCP socket.
set -euo pipefail
[[ "$EUID" -eq 0 ]] || { echo 'Run as root' >&2; exit 1; }
export DEBIAN_FRONTEND=noninteractive
apt-get update
apt-get install -y --no-install-recommends docker.io docker-compose-v2 openssl curl ca-certificates
systemctl enable --now docker
docker compose version
install -d -m 700 /etc/credenciales
# Ubuntu AWS AMIs ship SSM Agent as a snap. Fail rather than enable inbound SSH.
if systemctl list-unit-files snap.amazon-ssm-agent.amazon-ssm-agent.service --no-legend | grep -q amazon-ssm-agent; then
  systemctl enable --now snap.amazon-ssm-agent.amazon-ssm-agent.service
elif systemctl list-unit-files amazon-ssm-agent.service --no-legend | grep -q amazon-ssm-agent; then
  systemctl enable --now amazon-ssm-agent.service
else
  echo 'SSM Agent is missing from the selected Ubuntu AWS AMI' >&2
  exit 1
fi
