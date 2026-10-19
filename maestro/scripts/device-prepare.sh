#!/bin/bash
# Prepares a booted emulator for a test run: screen lock, animations, power,
# locale, then unlocks the keyguard.
#
# The app refuses to run without a device screen lock and wipes itself
# (MainActivityViewModel.validate), and its keystore key needs device
# authentication within the last 24 hours (KeyStoreManager), so the device
# gets a real PIN and is unlocked with it before every run.
#
# Usage: device-prepare.sh <serial>
set -euo pipefail

SERIAL="${1:?usage: device-prepare.sh <serial>}"
PIN="${E2E_DEVICE_PIN:-1111}"
adb_s() { adb -s "$SERIAL" "$@"; }

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

# Assertions match English text, so the locale is fixed. System properties
# need root, which google_apis images allow.
adb_s root >/dev/null
adb_s wait-for-device
adb_s shell setprop persist.sys.locale en-US
adb_s shell setprop persist.sys.timezone UTC

# Unlock the keyguard; this also restarts the keystore's 24-hour window
adb_s shell input keyevent KEYCODE_WAKEUP
adb_s shell wm dismiss-keyguard
sleep 1
adb_s shell input text "$PIN"
adb_s shell input keyevent KEYCODE_ENTER
sleep 1

if adb_s shell dumpsys window | grep -q "mDreamingLockscreen=true\|isKeyguardShowing=true\|mShowingLockscreen=true"; then
    echo "Keyguard is still showing on $SERIAL" >&2
    exit 1
fi

echo "Prepared $SERIAL"
