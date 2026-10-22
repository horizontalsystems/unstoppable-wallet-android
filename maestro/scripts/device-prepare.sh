#!/bin/bash
# Prepares a booted emulator for a test run: screen lock, animations, power,
# locale, then unlocks the keyguard.
#
# The app refuses to run without a device screen lock and wipes itself
# (MainActivityViewModel.validate), and its keystore key needs device
# authentication within the last 24 hours (KeyStoreManager), so the device
# gets a real PIN and is unlocked with it before every run.
#
# With an APK it also replaces the installed e2e build. It fails when the
# emulator has no internet or its clock is off, so those show up as setup
# errors rather than test failures.
#
# Usage: device-prepare.sh <serial> [apk]
set -euo pipefail
source "$(dirname "$0")/load-config.sh"

SERIAL="${1:?usage: device-prepare.sh <serial> [apk]}"
APK="${2:-}"
APP_ID="${APP_ID:-io.horizontalsystems.bankwallet.appcenter.e2e}"
PIN="${E2E_DEVICE_PIN:-1111}"
# The host adb server can drop a command while several emulators boot at once
# ("protocol fault"), so every call is retried
adb_s() {
    local attempt
    for attempt in $(seq 10); do
        adb -s "$SERIAL" "$@" && return 0
        sleep 3
    done
    return 1
}
trap 'echo "device-prepare.sh failed on $SERIAL at line $LINENO" >&2' ERR

adb_s wait-for-device
until [ "$(adb_s shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do
    sleep 2
done

# Setting the PIN fails when one is already set, which is fine
adb_s shell locksettings set-pin "$PIN" >/dev/null 2>&1 || true

adb_s shell settings put global window_animation_scale 0
adb_s shell settings put global transition_animation_scale 0
adb_s shell settings put global animator_duration_scale 0

adb_s shell svc power stayon true
adb_s shell settings put system screen_off_timeout 2147483647

# Assertions match English text. The image starts in en-US; changing the
# locale would need adb root, which restarts adbd and, with several
# emulators booting, left the host adb server dropping commands. So the
# locale is checked rather than set.
locale="$(adb_s shell getprop persist.sys.locale | tr -d '\r')"
[ -n "$locale" ] || locale="$(adb_s shell getprop ro.product.locale | tr -d '\r')"
if [ "$locale" != "en-US" ]; then
    echo "$SERIAL locale is $locale, expected en-US" >&2
    exit 1
fi
adb_s shell cmd alarm set-timezone UTC >/dev/null

# Unlock the keyguard; this also restarts the keystore's 24-hour window.
# Right after boot the PIN pad can take several seconds to appear, and a PIN
# typed before it is lost (it does not count as a wrong attempt), so the
# unlock is retried.
keyguard_showing() {
    adb_s shell dumpsys window | grep -q "mDreamingLockscreen=true\|isKeyguardShowing=true\|mShowingLockscreen=true"
}
for _ in $(seq 5); do
    adb_s shell input keyevent KEYCODE_WAKEUP
    adb_s shell wm dismiss-keyguard
    sleep 3
    adb_s shell input text "$PIN"
    adb_s shell input keyevent KEYCODE_ENTER
    sleep 3
    keyguard_showing || break
done

if keyguard_showing; then
    echo "Keyguard is still showing on $SERIAL. If the device PIN differs from E2E_DEVICE_PIN, change it with: adb -s $SERIAL shell locksettings set-pin --old <old> $PIN" >&2
    exit 1
fi

# The app goes by Android's own check of the default network ("validated"),
# not by the traffic itself. The emulator's Wi-Fi fails that check (its DNS
# lookups fail inside the system probe) while its mobile network passes, and
# Wi-Fi wins as the default, so the app showed "No Internet" on a working
# emulator. Wi-Fi is turned off to leave the mobile network as the default.
# Validation fetches a page over HTTP, so it also checks DNS and the internet
# connection; ping is not used because CI runners block ICMP.
adb_s shell svc wifi disable
default_validated() {
    local id
    id=$(adb_s shell dumpsys connectivity | sed -n 's/^Active default network: \([0-9]*\).*/\1/p' | tr -d '\r')
    [ -n "$id" ] && adb_s shell dumpsys connectivity | grep "network{$id}" | grep -q "IS_VALIDATED"
}
for _ in $(seq 60); do
    default_validated && break
    sleep 2
done
if ! default_validated; then
    echo "$SERIAL has no validated network (no internet, or DNS fails); the app would show No Internet" >&2
    exit 1
fi

# Chain sync and TLS fail on a wrong clock
device_time=$(adb_s shell date +%s | tr -d '\r')
drift=$(( device_time - $(date +%s) ))
if [ "${drift#-}" -gt 120 ]; then
    echo "$SERIAL clock is ${drift}s off the host" >&2
    exit 1
fi

if [ -n "$APK" ]; then
    adb_s uninstall "$APP_ID" >/dev/null 2>&1 || true
    adb_s install "$APK" >/dev/null
fi

echo "Prepared $SERIAL"
