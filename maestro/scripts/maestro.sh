#!/bin/bash
# Runs maestro with the settings from .env.e2e in the environment, so flows
# that need the dust wallet phrase work without exporting anything:
#   maestro/scripts/maestro.sh --device emulator-5554 test maestro/onchain
set -euo pipefail
source "$(dirname "$0")/load-config.sh"
export MAESTRO_CLI_NO_ANALYTICS=1 MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
exec "${MAESTRO:-$HOME/.maestro/bin/maestro}" "$@"
