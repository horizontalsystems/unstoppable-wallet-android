#!/bin/bash
# Fetches the shared Maestro flows from appkb into maestro/appkb/flows, at the
# tip of its flows branch, or at the ref given (another appkb branch or a
# commit). Uses your git credentials for appkb.
#
# Usage: fetch-flows.sh [ref]
set -euo pipefail
MAESTRO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$MAESTRO_DIR/appkb"
REF="${1:-flows}"
URL="${APPKB_URL:-https://github.com/horizontalsystems/appkb.git}"

if [ ! -d "$DEST/.git" ]; then
    git clone --quiet --filter=blob:none --no-checkout "$URL" "$DEST"
    git -C "$DEST" sparse-checkout set flows
fi
# checkout keeps an edit that does not conflict with the new commit, so the
# flows would then differ from the commit reported below
if [ -n "$(git -C "$DEST" status --porcelain)" ]; then
    echo "Uncommitted edits in ${DEST#"$PWD"/}, commit or discard them first:" >&2
    git -C "$DEST" status --short >&2
    exit 1
fi
git -C "$DEST" fetch --quiet origin "$REF"
git -C "$DEST" checkout --quiet --detach FETCH_HEAD
echo "Flows at $(git -C "$DEST" rev-parse --short HEAD) in ${DEST#"$PWD"/}/flows"
