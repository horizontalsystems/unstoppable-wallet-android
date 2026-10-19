#!/bin/bash
# Creates the AVD the Maestro suite runs on.
#
# API 35 with 4 KB pages, not the newer 16 KB-page images: the app's native
# libraries must load on it unchanged. arm64 because the APK ships ARM
# libraries only (app/build.gradle.kts abiFilters).
set -euo pipefail

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
AVD_NAME="${AVD_NAME:-e2e_api35}"
IMAGE="system-images;android-35;google_apis;arm64-v8a"

"$SDK/cmdline-tools/latest/bin/sdkmanager" "$IMAGE" >/dev/null

echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd \
    --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_6

CONFIG="$HOME/.android/avd/$AVD_NAME.avd/config.ini"
set_config() {
    if grep -q "^$1=" "$CONFIG"; then
        sed -i '' "s|^$1=.*|$1=$2|" "$CONFIG"
    else
        echo "$1=$2" >> "$CONFIG"
    fi
}
set_config hw.ramSize 4096
set_config disk.dataPartition.size 8G
set_config hw.audioInput no
set_config hw.audioOutput no
set_config hw.camera.back none
set_config hw.camera.front none

echo "Created AVD $AVD_NAME"
