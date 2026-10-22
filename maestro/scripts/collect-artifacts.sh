#!/bin/bash
# Makes a run directory safe to share: removes the dust wallet phrase from
# every text file, deletes the screenshots of on-chain flows (they restore the
# dust wallet, and FLAG_SECURE is off in the e2e build) and fails if the
# phrase can still be found anywhere.
#
# Maestro writes typed text in plain text to commands.json and maestro.log,
# so this runs before anything from the run directory is uploaded.
#
# Usage: collect-artifacts.sh <run dir>   (reads MAESTRO_E2E_DUST_MNEMONIC)
set -uo pipefail
source "$(dirname "$0")/load-config.sh"

RUN_DIR="${1:?usage: collect-artifacts.sh <run dir>}"
SECRET="${MAESTRO_E2E_DUST_MNEMONIC:-}"

# Every flow tagged secret is named "On-chain · ..."; the history tests use
# no secret and are named "History · ..."
find "$RUN_DIR" -path "*On-chain*" \( -name "*.png" -o -name "*.mp4" \) -delete 2>/dev/null

[ -n "$SECRET" ] || exit 0

export SECRET
grep -rlF -- "$SECRET" "$RUN_DIR" 2>/dev/null | while read -r file; do
    perl -pi -e 's/\Q$ENV{SECRET}\E/***/g' "$file"
done

if grep -rqF -- "$SECRET" "$RUN_DIR" 2>/dev/null; then
    echo "The dust wallet phrase is still in $RUN_DIR after redaction" >&2
    touch "$RUN_DIR/secret-leak"
    exit 1
fi
