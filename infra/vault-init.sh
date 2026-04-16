#!/bin/sh
echo "Waiting for vault to start..."
sleep 5

# Vault dev mode runs on port 8200
VAULT_URL="http://vault:8200"
VAULT_TOKEN="root"

echo "Attempting to write secrets to Vault..."

# We use Vault KV v2 API
curl -s \
  --header "X-Vault-Token: $VAULT_TOKEN" \
  --request POST \
  --data '{
    "data": {
      "DB_PASS": "pramod280504",
      "JWT_SECRET": "767BI36HSIUNYD7352198N1SV12BS62428748NDH7878BXB7B65D326B67SBVV6VS62545265776XBHG",
      "INTERNAL_SECRET": "JHGUY72638DEGWRT7D4365N89SOQL89234Y3IUTBR7DT4NQ3479387DNBYFGR366746823BETYX",
      "RABBITMQ_USERNAME": "guest",
      "RABBITMQ_PASSWORD": "guest",
      "RAZORPAY_KEY_ID": "rzp_live_SUvKCS3WVGspwp",
      "RAZORPAY_KEY_SECRET": "m0Cbz6xf3LuM3YzeDq6Kjv5L",
      "MAIL_PASSWORD": "vttkqfymujvgobzw"
    }
  }' \
  $VAULT_URL/v1/secret/data/application

echo "Secrets stored in Vault successfully!"
