#!/bin/bash
# Runs the journey when the newest version/* branch has a new head. launchd
# calls it every 10 minutes, so several commits in that time become one run
# of the newest one.
#
# A run that could not start because another holds the lock is retried on
# the next poll; every other outcome, including a failed build, marks the
# commit as tested so a broken head is reported once, not every 10 minutes.
set -uo pipefail

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
E2E_HOME="${E2E_HOME:-$HOME/e2e}"
REPO="${E2E_REPO:-https://github.com/horizontalsystems/unstoppable-wallet-android.git}"
LAST="$E2E_HOME/last-tested-sha"

line="$(git ls-remote --heads "$REPO" 'refs/heads/version/*' \
    | awk '{sub("refs/heads/", "", $2); print $2, $1}' | sort -V | tail -1)"
[ -n "$line" ] || { echo "No version/* branch found on $REPO" >&2; exit 2; }
branch="${line% *}"
sha="${line#* }"

if [ "$sha" = "$(cat "$LAST" 2>/dev/null)" ]; then
    exit 0
fi

echo "[$(date)] New head of $branch: ${sha:0:8}"
"$SCRIPTS/run-suite.sh" --repo "$REPO" --branch "$branch" --tags journey --trigger commit
status=$?
if [ $status -ne 3 ]; then
    mkdir -p "$E2E_HOME"
    echo "$sha" > "$LAST"
fi
exit $status
