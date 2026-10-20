#!/bin/bash
# Daily upkeep of the runner (launchd, 12:00):
#   - warns when no nightly run has finished for 36 hours (a dead Mac cannot
#     send this; then the missing ✅ is the signal)
#   - deletes run directories older than 30 days and Maestro's own output
#     older than 14 days
#   - warns when the disk has less than 20 GB free
#   - on Sundays, posts the flaky test digest
set -uo pipefail

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
E2E_HOME="${E2E_HOME:-$HOME/e2e}"
notify() { "$SCRIPTS/notify-telegram.sh" --text "$1"; }

last="$(cat "$E2E_HOME/last-nightly" 2>/dev/null || echo 0)"
if [ $(( $(date +%s) - last )) -gt $(( 36 * 3600 )) ]; then
    notify "⚠️ E2E: no nightly run has finished for over 36 hours. Check the runner Mac."
fi

find "$E2E_HOME/runs" -mindepth 1 -maxdepth 1 -type d -mtime +30 -exec rm -rf {} + 2>/dev/null
find "$HOME/.maestro/tests" -mindepth 1 -maxdepth 1 -type d -mtime +14 -exec rm -rf {} + 2>/dev/null

free_gb=$(df -g "$HOME" | awk 'NR == 2 {print $4}')
if [ "${free_gb:-0}" -lt 20 ]; then
    notify "⚠️ E2E runner: ${free_gb} GB free on disk."
fi

if [ "$(date +%u)" = "7" ]; then
    notify "$("$SCRIPTS/flaky-report.py" "$E2E_HOME/history/results.jsonl")"
fi
