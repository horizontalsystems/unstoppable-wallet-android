#!/bin/bash
# Runs one part of the suite on the emulator the GitHub workflow booted
# (.github/workflows/maestro_tests.yml): prepares it, installs the APK, runs
# the tests, retries failures once, and writes the summary.
#
#   journey  maestro/journey, rerun from the start if a step fails: each step
#            depends on the ones before it
#   flows    feature and on-chain tests selected by TAGS, failed ones rerun
#            one by one
#
# Results, JUnit reports and logs go to OUT_DIR, the summary also to
# $GITHUB_STEP_SUMMARY when set. Exit codes: 0 pass (flaky included),
# 1 failures, 2 setup error.
#
# Usage: ci-run.sh <journey|flows> <apk> <out dir> [tags, default feature,history]
set -uo pipefail
SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$SCRIPTS/../.." && pwd)"

PART="${1:?usage: ci-run.sh <journey|flows> <apk> <out dir> [tags]}"
APK="${2:?apk}"
OUT="${3:?out dir}"
TAGS="${4:-feature,history}"
mkdir -p "$OUT"

SERIAL="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1; exit }')"
[ -n "$SERIAL" ] || { echo "No emulator found" >&2; exit 2; }
"$SCRIPTS/device-prepare.sh" "$SERIAL" "$APK" || exit 2

adb -s "$SERIAL" logcat -c
adb -s "$SERIAL" logcat -v time >"$OUT/logcat.txt" 2>&1 &
LOGCAT_PID=$!
trap 'kill $LOGCAT_PID 2>/dev/null' EXIT

export MAESTRO_CLI_NO_ANALYTICS=1 MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
cd "$REPO"

run() {
    local name="$1"; shift
    maestro --device "$SERIAL" test --format junit --output "$OUT/$name.xml" \
        --test-output-dir "$OUT/$name" "$@" 2>&1 | tee "$OUT/$name.log" \
        | grep -E '^\[(Passed|Failed|Skipped)\]|Flows? (Passed|Failed)'
    return "${PIPESTATUS[0]}"
}

STATUS=0
case "$PART" in
    journey)
        if ! run journey maestro/journey; then
            echo "The journey failed; running it once more"
            run journey-retry maestro/journey || STATUS=1
        fi
        ;;
    flows)
        # The root config.yaml covers features/ and onchain/
        if ! run flows --include-tags "$TAGS" maestro/; then
            FAILED=()
            while read -r file; do [ -n "$file" ] && FAILED+=("$file"); done \
                < <(python3 "$SCRIPTS/junit.py" failed-files "$OUT/flows.xml")
            if [ ${#FAILED[@]} -gt 0 ]; then
                echo "Retrying ${#FAILED[@]} failed test(s)"
                run flows-retry "${FAILED[@]}" || STATUS=1
            else
                STATUS=1
            fi
        fi
        ;;
    *)
        echo "Unknown part: $PART" >&2
        exit 2
        ;;
esac

python3 "$SCRIPTS/junit.py" summary "$OUT" >"$OUT/summary.txt"
cat "$OUT/summary.txt"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
    {
        echo "### Maestro: $PART"
        echo '```'
        cat "$OUT/summary.txt"
        echo '```'
    } >>"$GITHUB_STEP_SUMMARY"
fi

exit $STATUS
