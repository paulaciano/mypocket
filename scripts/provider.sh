#!/usr/bin/env bash
# Payment provider simulator: signs and sends webhooks to wallet-core (ADR-0008).
#
#   scripts/provider.sh cash-in <account-number> <ARS|USD> <amount> [reference]
#
# Environment:
#   MYPOCKET_URL                      wallet-core base URL (default http://localhost:8080)
#   MYPOCKET_PROVIDER_WEBHOOK_SECRET  shared secret (default: the development example value, with a warning)
#
# Exits 0 when the webhook is accepted (2xx), 1 when it is rejected, 2 on invalid arguments, and with
# curl's exit code when wallet-core cannot be reached.
set -euo pipefail

base_url="${MYPOCKET_URL:-http://localhost:8080}"
if [[ -n "${MYPOCKET_PROVIDER_WEBHOOK_SECRET:-}" ]]; then
    secret="$MYPOCKET_PROVIDER_WEBHOOK_SECRET"
else
    secret="dev-example-provider-webhook-secret-32-bytes"
    echo "provider.sh: MYPOCKET_PROVIDER_WEBHOOK_SECRET not set, using the development example secret" >&2
fi

usage() {
    sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
    exit 2
}

fail() {
    echo "provider.sh: $*" >&2
    exit 2
}

# HMAC-SHA256 of "timestamp.body", hex encoded. The key is an openssl argument, visible to local users in
# the process list: acceptable for a development tool.
sign() {
    printf '%s.%s' "$1" "$2" | openssl dgst -sha256 -hmac "$secret" | awk '{print $NF}'
}

# Prints the response and the HTTP status; exits 1 when wallet-core rejects the webhook.
send() {
    local path="$1" body="$2" timestamp response status
    timestamp="$(date +%s)"
    response="$(curl -sS -X POST "$base_url$path" \
        -H 'Content-Type: application/json' \
        -H "X-Provider-Timestamp: $timestamp" \
        -H "X-Provider-Signature: sha256=$(sign "$timestamp" "$body")" \
        --data-raw "$body" \
        -w '\nHTTP %{http_code}')"
    echo "$response"
    status="${response##*HTTP }"
    [[ "$status" =~ ^2 ]] || exit 1
}

cash_in() {
    [[ $# -ge 3 && $# -le 4 ]] || usage
    local account="$1" currency="$2" amount="$3" reference="${4:-PRV-$(date +%s)-$RANDOM}"
    [[ "$account" =~ ^[0-9]{10}$ ]] || fail "account number must have 10 digits"
    [[ "$currency" =~ ^(ARS|USD)$ ]] || fail "currency must be ARS or USD"
    [[ "$amount" =~ ^[0-9]+(\.[0-9]{1,2})?$ ]] || fail "amount must look like 1500 or 1500.50"
    [[ "$reference" =~ ^[A-Za-z0-9._:-]{1,100}$ ]] || fail "reference must have 1 to 100 letters, digits or . _ : -"
    send /api/v1/webhooks/cash-ins \
        "{\"reference\":\"$reference\",\"accountNumber\":\"$account\",\"currency\":\"$currency\",\"amount\":\"$amount\"}"
}

command -v openssl >/dev/null || fail "openssl is required"
command -v curl >/dev/null || fail "curl is required"

case "${1:-}" in
    cash-in) shift; cash_in "$@" ;;
    *) usage ;;
esac
