#!/bin/bash
# Stops the given emulators. Read-only instances discard their changes.
#
# Usage: emulators-down.sh <serial>...
set -uo pipefail

for serial in "$@"; do
    adb -s "$serial" emu kill >/dev/null 2>&1 && echo "Stopped $serial" >&2
done
