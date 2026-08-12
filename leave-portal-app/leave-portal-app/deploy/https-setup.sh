#!/usr/bin/env bash
# HTTPS setup for leave.sutuaa.com on an OCI Ubuntu/Debian instance.
#
# Prerequisites:
#   1. leave.sutuaa.com must resolve (A record) to this instance's PUBLIC IP.
#   2. OCI security list / VCN rules must allow inbound TCP 80 and TCP 443.
#   3. The Spring Boot app must be running locally on 127.0.0.1:8080.
#
# Usage (run on the VM via PuTTY):
#   sudo bash deploy/https-setup.sh
set -euo pipefail

DOMAIN="leave.sutuaa.com"
APP_PORT="${APP_PORT:-8080}"
APP_HOST="${APP_HOST:-127.0.0.1}"

if [ "$(id -u)" -ne 0 ]; then
  echo "Please run as root: sudo bash deploy/https-setup.sh" >&2
  exit 1
fi

echo "==> Installing nginx + certbot..."
apt-get update -y
apt-get install -y nginx certbot python3-certbot-nginx

echo "==> Writing nginx site config for $DOMAIN..."
cat > /etc/nginx/sites-available/leave-portal <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name ${DOMAIN};

    location / {
        proxy_pass http://${APP_HOST}:${APP_PORT};
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 300s;
        client_max_body_size 10m;
    }
}
EOF

ln -sf /etc/nginx/sites-available/leave-portal /etc/nginx/sites-enabled/leave-portal
rm -f /etc/nginx/sites-enabled/default

echo "==> Validating nginx config..."
nginx -t
systemctl enable nginx
systemctl restart nginx

echo "==> Requesting Let's Encrypt certificate for $DOMAIN..."
# --nginx automatically adds the 443 ssl block and the HTTP -> HTTPS redirect.
certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos -m "admin@${DOMAIN}" --redirect

systemctl reload nginx

echo ""
echo "=== DONE ==="
echo "HTTPS is live at: https://${DOMAIN}"
echo ""
echo "Next steps:"
echo "  1. In Google Cloud Console, add this EXACT authorized redirect URI:"
echo "       https://${DOMAIN}/login/oauth2/code/google"
echo "  2. Make sure the app is running: java -jar leave-portal-app.jar"
echo "  3. Verify: https://${DOMAIN}  -> login page -> Google sign-in."
