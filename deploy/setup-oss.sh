#!/usr/bin/env bash
set -euo pipefail

: "${OSS_DEPLOY_HOST:?set OSS_DEPLOY_HOST}"
: "${MINIO_ROOT_USER:?set MINIO_ROOT_USER}"
: "${MINIO_ROOT_PASSWORD:?set MINIO_ROOT_PASSWORD}"

echo "=== OSS (MinIO) Setup for Test Server ==="
echo "Target: ${OSS_DEPLOY_HOST}"
echo ""

# ---------- upload ----------
echo "[1/3] Uploading deploy files..."
scp deploy/docker-compose.yml "root@${OSS_DEPLOY_HOST}:/root/oss/"

# ---------- install docker ----------
echo "[2/3] Installing Docker (if needed)..."
ssh "root@${OSS_DEPLOY_HOST}" << 'EOF'
  if ! command -v docker &>/dev/null; then
    curl -fsSL https://get.docker.com | sh
    systemctl enable docker && systemctl start docker
  fi
  if ! command -v docker-compose &>/dev/null && ! docker compose version &>/dev/null; then
    curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    chmod +x /usr/local/bin/docker-compose
  fi
EOF

# ---------- start minio ----------
echo "[3/3] Starting MinIO..."
ssh "root@${OSS_DEPLOY_HOST}" bash -s -- "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" "$OSS_DEPLOY_HOST" << 'EOF'
  MINIO_ROOT_USER="$1"
  MINIO_ROOT_PASSWORD="$2"
  OSS_DEPLOY_HOST="$3"

  cd /root/oss
  cat > .env << ENV
MINIO_ROOT_USER=${MINIO_ROOT_USER}
MINIO_ROOT_PASSWORD=${MINIO_ROOT_PASSWORD}
ENV
  docker compose up -d
  echo ""
  echo "MinIO S3 API:   http://${OSS_DEPLOY_HOST}:9000"
  echo "MinIO Console:  http://${OSS_DEPLOY_HOST}:9001"
  echo "AccessKey: ${MINIO_ROOT_USER}"
EOF

echo ""
echo "=== Done ==="
