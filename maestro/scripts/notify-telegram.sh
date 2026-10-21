#!/bin/bash
# Reports a run to the team Telegram chat through the Bot API.
#
#   failed run (any trigger)  ❌ with counts, failed flows and a screenshot
#   setup failure             ⚠️ with the step that failed
#   nightly success           ✅ one line, as a daily heartbeat
#   other success with flaky  🟡 naming the flaky tests
#   other successes           nothing
#
# The token and chat id come from TELEGRAM_TOKEN and TELEGRAM_CHAT_ID (the
# environment or .env.e2e) or from the Keychain (uw-e2e telegram_token and
# telegram_chat_id). Without them the message is printed instead of sent.
#
# Usage: notify-telegram.sh <run dir> <trigger: nightly|commit|manual>
#        notify-telegram.sh --text <message>
set -uo pipefail
source "$(dirname "$0")/load-config.sh"

RUN_DIR="${1:?usage: notify-telegram.sh <run dir> <trigger> | --text <message>}"
TRIGGER="${2:-manual}"

TOKEN="${TELEGRAM_TOKEN:-$(security find-generic-password -s uw-e2e -a telegram_token -w 2>/dev/null)}"
CHAT="${TELEGRAM_CHAT_ID:-$(security find-generic-password -s uw-e2e -a telegram_chat_id -w 2>/dev/null)}"

meta() { sed -n "s/.*\"$1\": \"\{0,1\}\([^\",]*\).*/\1/p" "$RUN_DIR/meta.json" 2>/dev/null | head -1; }
branch="$(meta branch)"
sha="$(meta sha)"
where="$branch ${sha:0:8}"
passed=$(grep -c '^\[Passed' "$RUN_DIR/summary.txt" 2>/dev/null)
failed=$(grep -c '^\[Failed' "$RUN_DIR/summary.txt" 2>/dev/null)
flaky=$(grep -c '^\[Flaky' "$RUN_DIR/summary.txt" 2>/dev/null)
flaky_names="$(sed -n 's/^\[Flaky\] \(.*\) ([0-9].*/• \1 (flaky)/p' "$RUN_DIR/summary.txt" 2>/dev/null)"
photo=""

if [ "$RUN_DIR" = "--text" ]; then
    text="$TRIGGER"
elif [ -f "$RUN_DIR/setup-error.txt" ]; then
    text="⚠️ E2E setup failed ($TRIGGER) on $where: $(cat "$RUN_DIR/setup-error.txt")"
elif [ -f "$RUN_DIR/secret-leak" ]; then
    text="⚠️ E2E run on $where left the dust wallet phrase in its output; nothing else was sent. Check $RUN_DIR"
elif [ "${failed:-0}" -gt 0 ]; then
    names="$(sed -n 's/^\[Failed\] \(.*\) ([0-9].*/• \1/p' "$RUN_DIR/summary.txt")"
    text="$(printf '❌ E2E %s on %s: %s passed, %s flaky, %s failed\n%s\n%s\n%s' \
        "$TRIGGER" "$where" "$passed" "$flaky" "$failed" "$names" "$flaky_names" "$RUN_DIR")"
    photo="$(find "$RUN_DIR" -name "*.png" -path "*screenshots*" 2>/dev/null | head -1)"
elif [ "$TRIGGER" != "nightly" ] && [ "${flaky:-0}" -gt 0 ]; then
    text="$(printf '🟡 E2E %s on %s passed with %s flaky\n%s' "$TRIGGER" "$where" "$flaky" "$flaky_names")"
elif [ "$TRIGGER" = "nightly" ]; then
    text="$(printf '✅ E2E nightly on %s: %s passed, %s flaky\n%s' "$where" "$passed" "$flaky" "$flaky_names")"
else
    exit 0
fi

if [ -z "$TOKEN" ] || [ -z "$CHAT" ]; then
    echo "Telegram is not configured; message:" >&2
    echo "$text" >&2
    exit 0
fi

# The URL holds the token, so curl reads it from stdin rather than from its
# arguments, which other users can see in the process list
api="https://api.telegram.org/bot$TOKEN"
echo "url = \"$api/sendMessage\"" | curl -fsS -o /dev/null -K - \
    --data-urlencode "chat_id=$CHAT" --data-urlencode "text=$text" \
    || echo "Telegram sendMessage failed" >&2
if [ -n "$photo" ]; then
    echo "url = \"$api/sendPhoto\"" | curl -fsS -o /dev/null -K - \
        -F "chat_id=$CHAT" -F "photo=@$photo" \
        || echo "Telegram sendPhoto failed" >&2
fi
