#!/bin/bash
# The single entry point for a suite run: launchd, developers and manual
# reruns all use it.
#
# Takes a lock, checks out the branch in a clone of its own, builds the e2e
# APK, boots read-only emulators, runs the journey on the first one and the
# feature and on-chain tests split across the others, and writes the results
# to $E2E_HOME/runs/<timestamp>-<sha8>/. The emulators are stopped and the
# lock released however the run ends.
#
# Usage: run-suite.sh [--branch <name>] [--tags journey,feature,onchain]
#                     [--shards <n>] [--repo <url or path>] [--local <dir>]
#                     [--trigger nightly|commit|manual]
#
#   --branch  branch to test; default: the newest version/* on the remote
#   --tags    which tests to run; default: journey
#   --shards  emulators for feature and on-chain tests; default: 2
#   --repo    repository to clone; default: the public GitHub repository
#   --local   test this working copy as it is, without checkout (for
#             developers)
#   --trigger what started the run, for the Telegram report; default: manual
#
# Exit codes: 0 all passed, 1 test failures, 2 setup failed (lock, checkout,
# build, emulators), 3 another run holds the lock.
set -uo pipefail

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
source "$SCRIPTS/load-config.sh"
E2E_HOME="${E2E_HOME:-$HOME/e2e}"
REPO="${E2E_REPO:-https://github.com/horizontalsystems/unstoppable-wallet-android.git}"
BRANCH=""
TAGS="journey"
SHARDS=2
LOCAL=""
TRIGGER="manual"

while [ $# -gt 0 ]; do
    case "$1" in
        --branch) BRANCH="$2"; shift 2 ;;
        --tags) TAGS="$2"; shift 2 ;;
        --shards) SHARDS="$2"; shift 2 ;;
        --repo) REPO="$2"; shift 2 ;;
        --local) LOCAL="$(cd "$2" && pwd)"; shift 2 ;;
        --trigger) TRIGGER="$2"; shift 2 ;;
        *) echo "Unknown option $1" >&2; exit 2 ;;
    esac
done

has_tag() { [[ ",$TAGS," == *",$1,"* ]]; }

log() { echo "[$(date +%H:%M:%S)] $*" >&2; }
setup_failed() {
    log "Setup failed: $*"
    [ -n "${RUN_DIR:-}" ] && echo "$*" > "$RUN_DIR/setup-error.txt"
    exit 2
}

mkdir -p "$E2E_HOME/runs"

# mkdir is atomic, so two runs started together cannot both get the lock
LOCK="$E2E_HOME/run.lock"
if ! mkdir "$LOCK" 2>/dev/null; then
    log "Another run is in progress ($LOCK)"
    exit 3
fi

SERIALS=()
LOGCAT_PIDS=()
cleanup() {
    if [ -n "${RUN_DIR:-}" ]; then
        "$SCRIPTS/collect-artifacts.sh" "$RUN_DIR"
        "$SCRIPTS/notify-telegram.sh" "$RUN_DIR" "$TRIGGER"
    fi
    for pid in ${LOGCAT_PIDS[@]+"${LOGCAT_PIDS[@]}"}; do kill "$pid" 2>/dev/null; done
    [ ${#SERIALS[@]} -gt 0 ] && "$SCRIPTS/emulators-down.sh" "${SERIALS[@]}"
    rmdir "$LOCK" 2>/dev/null
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# --- Checkout ---------------------------------------------------------------
if [ -n "$LOCAL" ]; then
    CHECKOUT="$LOCAL"
    BRANCH="$(git -C "$CHECKOUT" rev-parse --abbrev-ref HEAD) (local)"
else
    CHECKOUT="$E2E_HOME/checkout"
    if [ ! -d "$CHECKOUT/.git" ]; then
        git clone --quiet "$REPO" "$CHECKOUT" || setup_failed "clone $REPO"
    fi
    git -C "$CHECKOUT" remote set-url origin "$REPO"
    git -C "$CHECKOUT" fetch --quiet --prune origin || setup_failed "fetch"
    if [ -z "$BRANCH" ]; then
        BRANCH="$(git -C "$CHECKOUT" for-each-ref --format='%(refname:lstrip=3)' 'refs/remotes/origin/version/*' \
            | sort -V | tail -1)"
        [ -n "$BRANCH" ] || setup_failed "no version/* branch on the remote"
    fi
    git -C "$CHECKOUT" checkout --quiet --detach "origin/$BRANCH" || setup_failed "checkout $BRANCH"
    # Gradle caches live in ~/.gradle, so a clean checkout keeps them
    git -C "$CHECKOUT" clean -fdxq
fi
SHA="$(git -C "$CHECKOUT" rev-parse HEAD)"

RUN_DIR="$E2E_HOME/runs/$(date +%Y%m%d-%H%M%S)-${SHA:0:8}"
mkdir -p "$RUN_DIR"
ln -sfn "$RUN_DIR" "$E2E_HOME/runs/latest"
STARTED="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
write_meta() {
    cat >"$RUN_DIR/meta.json" <<JSON
{
  "branch": "$BRANCH",
  "sha": "$SHA",
  "tags": "$TAGS",
  "trigger": "$TRIGGER",
  "started": "$STARTED",
  "finished": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "maestro": "$("${MAESTRO:-$HOME/.maestro/bin/maestro}" --version 2>/dev/null | tail -1)",
  "emulators": "${SERIALS[*]:-}",
  "journey_status": ${JOURNEY_STATUS:-null},
  "flows_status": ${FLOWS_STATUS:-null}
}
JSON
}
write_meta
log "Testing $BRANCH at ${SHA:0:8}, tags $TAGS, results in $RUN_DIR"

# --- Build ------------------------------------------------------------------
log "Building"
(cd "$CHECKOUT" && ./gradlew --quiet :app:assembleCiE2e) >"$RUN_DIR/build.log" 2>&1 \
    || setup_failed "build (see build.log)"
APK="$CHECKOUT/app/build/outputs/apk/ci/e2e/app-ci-e2e.apk"

# --- Secrets ----------------------------------------------------------------
# From .env.e2e (loaded above) or the Keychain; MAESTRO_ variables reach the
# flows without -e, so the phrase never appears in process arguments.
if has_tag onchain && [ -z "${MAESTRO_E2E_DUST_MNEMONIC:-}" ]; then
    MAESTRO_E2E_DUST_MNEMONIC="$(security find-generic-password -s uw-e2e -a dust_mnemonic -w 2>/dev/null)" \
        || setup_failed "MAESTRO_E2E_DUST_MNEMONIC is not in .env.e2e or the Keychain (see maestro/README.md, Secrets)"
    export MAESTRO_E2E_DUST_MNEMONIC
fi

# --- Emulators --------------------------------------------------------------
FLOW_DEVICES=0
if has_tag feature || has_tag onchain; then FLOW_DEVICES=$SHARDS; fi
JOURNEY_DEVICES=0
has_tag journey && JOURNEY_DEVICES=1
COUNT=$((JOURNEY_DEVICES + FLOW_DEVICES))
[ $COUNT -gt 0 ] || setup_failed "no tests selected by --tags $TAGS"

log "Starting $COUNT emulator(s)"
while read -r serial; do SERIALS+=("$serial"); done < <("$SCRIPTS/emulators-up.sh" "$COUNT")
[ ${#SERIALS[@]} -eq "$COUNT" ] || setup_failed "emulators did not start"

# The adb server restarts a few times while new emulators come up; wait
# until every instance has been listed as a device for 10 seconds in a row
steady=0
for _ in $(seq 60); do
    online=$(adb devices | grep -cE "^($(IFS='|'; echo "${SERIALS[*]}"))\s+device$")
    if [ "$online" -eq "$COUNT" ]; then steady=$((steady + 1)); else steady=0; fi
    [ $steady -ge 5 ] && break
    sleep 2
done

for serial in "${SERIALS[@]}"; do
    "$SCRIPTS/device-prepare.sh" "$serial" "$APK" >&2 || setup_failed "prepare $serial"
    adb -s "$serial" logcat -c
    adb -s "$serial" logcat -v time >"$RUN_DIR/logcat-$serial.txt" 2>&1 &
    LOGCAT_PIDS+=($!)
done

# --- Run --------------------------------------------------------------------
export MAESTRO_CLI_NO_ANALYTICS=1 MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
MAESTRO="${MAESTRO:-$HOME/.maestro/bin/maestro}"
cd "$CHECKOUT"

run_journey() {
    "$MAESTRO" --device "${SERIALS[0]}" test --format junit --output "$RUN_DIR/journey.xml" \
        --test-output-dir "$RUN_DIR/journey" maestro/journey >"$RUN_DIR/journey.log" 2>&1
}

run_flows() {
    local devices=("${SERIALS[@]:$JOURNEY_DEVICES}")
    local device_list
    device_list="$(IFS=,; echo "${devices[*]}")"
    # The root config covers features and on-chain tests; a single tag narrows it
    local filter=()
    if ! has_tag feature; then filter=(--include-tags onchain); fi
    if ! has_tag onchain; then filter=(--include-tags feature); fi
    "$MAESTRO" --device "$device_list" test --shard-split "${#devices[@]}" ${filter[@]+"${filter[@]}"} \
        --format junit --output "$RUN_DIR/flows.xml" --test-output-dir "$RUN_DIR/flows" \
        maestro/ >"$RUN_DIR/flows.log" 2>&1
}

JOURNEY_STATUS=0
FLOWS_STATUS=0
if [ $JOURNEY_DEVICES -eq 1 ]; then
    log "Running the journey on ${SERIALS[0]}"
    run_journey &
    JOURNEY_PID=$!
fi
if [ $FLOW_DEVICES -gt 0 ]; then
    log "Running feature and on-chain tests on ${SERIALS[*]:$JOURNEY_DEVICES}"
    run_flows || FLOWS_STATUS=1
fi
if [ -n "${JOURNEY_PID:-}" ]; then
    wait "$JOURNEY_PID" || JOURNEY_STATUS=1
fi

# --- Retry ------------------------------------------------------------------
# One retry, and a test that passes on it is reported as flaky. Journey steps
# depend on the ones before them, so a failed journey reruns from the start;
# feature and on-chain tests rerun one by one.
wait_online() {
    local attempt
    for attempt in $(seq 12); do
        adb -s "$1" shell ping -c 1 -W 5 connectivitycheck.gstatic.com >/dev/null 2>&1 && return 0
        sleep 5
    done
    log "$1 has no internet connection"
}

if [ $JOURNEY_STATUS -ne 0 ] && [ $JOURNEY_DEVICES -eq 1 ]; then
    log "The journey failed; running it once more"
    wait_online "${SERIALS[0]}"
    "$MAESTRO" --device "${SERIALS[0]}" test --format junit --output "$RUN_DIR/journey-retry.xml" \
        --test-output-dir "$RUN_DIR/journey-retry" maestro/journey >"$RUN_DIR/journey-retry.log" 2>&1 \
        && JOURNEY_STATUS=0
fi

if [ $FLOWS_STATUS -ne 0 ]; then
    FAILED_FLOWS=()
    while read -r file; do [ -n "$file" ] && FAILED_FLOWS+=("$file"); done \
        < <("$SCRIPTS/junit.py" failed-files "$RUN_DIR/flows.xml")
    if [ ${#FAILED_FLOWS[@]} -gt 0 ]; then
        log "Retrying ${#FAILED_FLOWS[@]} failed test(s)"
        RETRY_DEVICES=("${SERIALS[@]:$JOURNEY_DEVICES}")
        for serial in "${RETRY_DEVICES[@]}"; do wait_online "$serial"; done
        "$MAESTRO" --device "$(IFS=,; echo "${RETRY_DEVICES[*]}")" test \
            --shard-split "$(( ${#FAILED_FLOWS[@]} < ${#RETRY_DEVICES[@]} ? ${#FAILED_FLOWS[@]} : ${#RETRY_DEVICES[@]} ))" \
            --format junit --output "$RUN_DIR/flows-retry.xml" --test-output-dir "$RUN_DIR/flows-retry" \
            "${FAILED_FLOWS[@]}" >"$RUN_DIR/flows-retry.log" 2>&1 \
            && FLOWS_STATUS=0
    fi
fi

# --- Report -----------------------------------------------------------------
write_meta

"$SCRIPTS/junit.py" summary "$RUN_DIR" >"$RUN_DIR/summary.txt"
"$SCRIPTS/junit.py" history "$RUN_DIR" "$E2E_HOME/history/results.jsonl"
[ "$TRIGGER" = "nightly" ] && date +%s > "$E2E_HOME/last-nightly"
log "$(grep -c '^\[Passed' "$RUN_DIR/summary.txt") passed, $(grep -c '^\[Flaky' "$RUN_DIR/summary.txt") flaky, $(grep -c '^\[Failed' "$RUN_DIR/summary.txt") failed"

[ $JOURNEY_STATUS -eq 0 ] && [ $FLOWS_STATUS -eq 0 ]
exit $?
