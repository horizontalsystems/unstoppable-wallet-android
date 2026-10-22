#!/bin/bash
# Reports a GitHub Actions run of the suite to the team Telegram chat.
#
#   setup failure      ⚠️ naming the part and the step that failed
#   failed tests       ❌ with counts and the failed flows
#   nightly success    ✅ with counts, as a daily heartbeat
#   other successes    nothing
#
# RESULTS_DIR holds one folder per part (journey, flows), each with the
# summary.txt and, after a setup failure, the setup-error.txt that ci-run.sh
# writes. BRANCH, SHA and RUN_URL describe the run. The token and chat id come
# from TELEGRAM_TOKEN and TELEGRAM_CHAT_ID; without them the message is
# printed instead of sent.
#
# Usage: notify-telegram.sh <results dir> <trigger: nightly|push|manual>
set -uo pipefail
source "$(dirname "$0")/load-config.sh"

RESULTS="${1:?usage: notify-telegram.sh <results dir> <trigger>}"
TRIGGER="${2:-manual}"
TOKEN="${TELEGRAM_TOKEN:-}"
CHAT="${TELEGRAM_CHAT_ID:-}"
where="${BRANCH:-?} ${SHA:0:8}"

count() { cat "$RESULTS"/*/summary.txt 2>/dev/null | grep -c "^\[$1\]"; }
names() { cat "$RESULTS"/*/summary.txt 2>/dev/null | sed -n "s/^\[$1\] \(.*\) ([0-9].*/• \1$2/p"; }
passed=$(count Passed)
failed=$(count Failed)
flaky=$(count Flaky)

setup_errors=""
for part in journey flows; do
    if [ -f "$RESULTS/$part/setup-error.txt" ]; then
        setup_errors+="• $part: $(cat "$RESULTS/$part/setup-error.txt")"$'\n'
    elif [ ! -f "$RESULTS/$part/summary.txt" ]; then
        setup_errors+="• $part: no results (the job stopped before the tests)"$'\n'
    fi
done

if [ -n "$setup_errors" ]; then
    text="$(printf '⚠️ E2E %s on %s: setup failed\n%s%s passed, %s failed\n%s' \
        "$TRIGGER" "$where" "$setup_errors" "$passed" "$failed" "${RUN_URL:-}")"
elif [ "$failed" -gt 0 ]; then
    text="$(printf '❌ E2E %s on %s: %s passed, %s flaky, %s failed\n%s\n%s\n%s' \
        "$TRIGGER" "$where" "$passed" "$flaky" "$failed" \
        "$(names Failed '')" "$(names Flaky ' (flaky)')" "${RUN_URL:-}")"
elif [ "$TRIGGER" = "nightly" ]; then
    text="$(printf '✅ E2E nightly on %s: %s passed, %s flaky\n%s\n%s' \
        "$where" "$passed" "$flaky" "$(names Flaky ' (flaky)')" "${RUN_URL:-}")"
else
    exit 0
fi

if [ -z "$TOKEN" ] || [ -z "$CHAT" ]; then
    echo "Telegram is not configured; message:" >&2
    echo "$text" >&2
    exit 0
fi

# The URL holds the token, so curl reads it from stdin rather than from its
# arguments, which other processes can see
echo "url = \"https://api.telegram.org/bot$TOKEN/sendMessage\"" | curl -fsS -o /dev/null -K - \
    --data-urlencode "chat_id=$CHAT" --data-urlencode "text=$text" \
    --data-urlencode "disable_web_page_preview=true" \
    || { echo "Telegram sendMessage failed" >&2; exit 1; }
