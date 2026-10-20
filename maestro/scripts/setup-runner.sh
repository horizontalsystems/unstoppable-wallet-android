#!/bin/bash
# Brings up the runner on a fresh macOS user (the "ci" user on the runner
# Mac): installs the toolchain at the versions in maestro/README.md, creates
# the AVD, copies these scripts to $E2E_HOME/bin and installs the launchd
# agents.
#
# The scripts run from $E2E_HOME/bin, not from the checkout, because
# run-suite.sh checks out other branches in its clone and would replace
# itself mid-run. Rerun this script after changing them.
#
# The secrets are stored by hand, once (see maestro/README.md, Secrets).
#
# Usage: setup-runner.sh [--dry-run]
#   --dry-run  only print the launchd agents it would install
set -euo pipefail

SCRIPTS="$(cd "$(dirname "$0")" && pwd)"
E2E_HOME="${E2E_HOME:-$HOME/e2e}"
DRY_RUN=false
[ "${1:-}" = "--dry-run" ] && DRY_RUN=true

MAESTRO_VERSION="2.11.0"
SYSTEM_IMAGE="system-images;android-35;google_apis;arm64-v8a"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home}"
AGENT_PATH="$HOME/.maestro/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$JAVA_HOME/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

render_agent() {
    sed -e "s|__SCRIPTS__|$E2E_HOME/bin|g" \
        -e "s|__PATH__|$AGENT_PATH|g" \
        -e "s|__ANDROID_HOME__|$ANDROID_HOME|g" \
        -e "s|__JAVA_HOME__|$JAVA_HOME|g" \
        -e "s|__E2E_HOME__|$E2E_HOME|g" "$1"
}

if $DRY_RUN; then
    for template in "$SCRIPTS"/launchd/*.plist; do
        echo "== $(basename "$template")"
        render_agent "$template"
    done
    exit 0
fi

# --- Toolchain --------------------------------------------------------------
command -v brew >/dev/null || { echo "Install Homebrew first: https://brew.sh" >&2; exit 1; }
brew list --cask temurin@17 >/dev/null 2>&1 || brew install --cask temurin@17
brew list --cask android-commandlinetools >/dev/null 2>&1 || brew install --cask android-commandlinetools

mkdir -p "$ANDROID_HOME"
SDKMANAGER="$(command -v sdkmanager || echo "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager")"
yes | "$SDKMANAGER" --sdk_root="$ANDROID_HOME" --licenses >/dev/null || true
"$SDKMANAGER" --sdk_root="$ANDROID_HOME" "platform-tools" "emulator" "$SYSTEM_IMAGE" \
    "cmdline-tools;latest" >/dev/null

if [ "$("$HOME/.maestro/bin/maestro" --version 2>/dev/null | tail -1)" != "$MAESTRO_VERSION" ]; then
    curl -fsSL https://get.maestro.mobile.dev -o "${TMPDIR:-/tmp}/maestro-install.sh"
    MAESTRO_VERSION="$MAESTRO_VERSION" bash "${TMPDIR:-/tmp}/maestro-install.sh"
fi

# --- AVD --------------------------------------------------------------------
ANDROID_HOME="$ANDROID_HOME" "$SCRIPTS/avd-create.sh"

# --- Scripts and agents -----------------------------------------------------
mkdir -p "$E2E_HOME/bin" "$E2E_HOME/logs" "$E2E_HOME/runs"
cp "$SCRIPTS"/*.sh "$SCRIPTS"/*.py "$E2E_HOME/bin/"
chmod +x "$E2E_HOME"/bin/*

mkdir -p "$HOME/Library/LaunchAgents"
for template in "$SCRIPTS"/launchd/*.plist; do
    agent="$HOME/Library/LaunchAgents/$(basename "$template")"
    render_agent "$template" > "$agent"
    launchctl bootout "gui/$(id -u)" "$agent" 2>/dev/null || true
    launchctl bootstrap "gui/$(id -u)" "$agent"
done

echo "Runner set up. Agents: $(launchctl list | grep -c io.horizontalsystems.e2e) loaded."
echo "Store the secrets next if you have not (maestro/README.md, Secrets)."
