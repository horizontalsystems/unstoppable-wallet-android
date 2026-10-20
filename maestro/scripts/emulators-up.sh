#!/bin/bash
# Starts N headless, read-only instances of the e2e AVD and prints their
# serials, one per line, once they have booted. Progress goes to stderr.
#
# Read-only instances share one AVD and discard every change on exit, so each
# run starts from the same state. They boot from the "clean" snapshot when
# avd-create.sh saved one, and cold otherwise.
#
# Ports start at 5580 so the instances never take the ports of emulators a
# developer has open (5554, 5556, ...).
#
# Usage: emulators-up.sh <count>
set -euo pipefail

COUNT="${1:?usage: emulators-up.sh <count>}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
AVD_NAME="${AVD_NAME:-e2e_api35}"
EMULATOR="$SDK/emulator/emulator"
FIRST_PORT="${E2E_FIRST_PORT:-5580}"
BOOT_TIMEOUT="${E2E_BOOT_TIMEOUT:-300}"
# Fixed resolvers: with the host's, name lookups inside the emulator failed
# now and then while plain IP traffic still worked, and the app then showed
# "No Internet"
DNS_SERVERS="${E2E_DNS_SERVERS:-8.8.8.8,1.1.1.1}"

# A writable instance holds a lock on the AVD, and read-only ones then fail
if ps -axo command | grep -E "qemu-system[^ ]* .*-avd $AVD_NAME( |$)" | grep -v -- "-read-only" | grep -qv grep; then
    echo "$AVD_NAME is already running without -read-only; stop it first" >&2
    exit 1
fi

SNAPSHOT_ARGS=(-no-snapshot-load)
if [ -d "$HOME/.android/avd/$AVD_NAME.avd/snapshots/clean" ]; then
    SNAPSHOT_ARGS=(-snapshot clean)
fi

port_in_use() { lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1; }

serials=()
booted=false
# Stop what was started if the script fails before handing the serials over
trap '$booted || "$(dirname "$0")/emulators-down.sh" ${serials[@]+"${serials[@]}"}' EXIT
port=$FIRST_PORT
while [ ${#serials[@]} -lt "$COUNT" ]; do
    if port_in_use "$port" || port_in_use $((port + 1)); then
        port=$((port + 2))
        continue
    fi
    echo "Starting $AVD_NAME on port $port" >&2
    nohup "$EMULATOR" -avd "$AVD_NAME" -read-only "${SNAPSHOT_ARGS[@]}" -no-snapshot-save \
        -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect -port "$port" \
        -dns-server "$DNS_SERVERS" \
        >"${TMPDIR:-/tmp}/emulator-$port.log" 2>&1 &
    serials+=("emulator-$port")
    port=$((port + 2))
done

deadline=$((SECONDS + BOOT_TIMEOUT))
for serial in "${serials[@]}"; do
    until [ "$(adb -s "$serial" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
        if [ $SECONDS -ge $deadline ]; then
            echo "$serial did not boot within ${BOOT_TIMEOUT}s" >&2
            exit 1
        fi
        sleep 3
    done
    echo "Booted $serial" >&2
done

booted=true
printf '%s\n' "${serials[@]}"
