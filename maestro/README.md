# Maestro UI Tests

## Setup

Tests run against the `ci` flavor, the dev build the team uses, on an arm64
emulator: locally, and on the test station
([horizontalsystems/uw-testlab](https://github.com/horizontalsystems/uw-testlab)),
a Mac mini in the office that builds the app and runs the suite.

| Tool | Version |
|---|---|
| Maestro CLI | 2.11.0 |
| System image | `system-images;android-35;google_apis;arm64-v8a` |
| JDK (for the build) | Temurin 17 |

Use API 35, not the 16 KB-page images (`*_ps16k`): the app's native libraries
are built for 4 KB pages.

1. Install Maestro at the pinned version:

   ```bash
   curl -fsSL https://get.maestro.mobile.dev -o maestro-install.sh
   MAESTRO_VERSION=2.11.0 bash maestro-install.sh
   ```

2. Turn off Maestro's analytics and banner (add to your shell profile):

   ```bash
   export MAESTRO_CLI_NO_ANALYTICS=1
   export MAESTRO_CLI_ANALYSIS_NOTIFICATION_DISABLED=true
   ```

3. Create the AVD, boot it and prepare it:

   ```bash
   maestro/scripts/avd-create.sh
   emulator -avd e2e_api35 -no-window -no-audio -no-boot-anim -no-snapshot -port 5554 &
   maestro/scripts/device-prepare.sh emulator-5554
   ```

   `device-prepare.sh` sets a device screen lock (PIN `1111`), turns
   animations off, keeps the screen on, fixes the locale to en-US, and unlocks
   the keyguard. The app refuses to run without a screen lock, so this step is
   required after every boot.

4. Build and install the app:

   ```bash
   ./gradlew :app:assembleCiE2e
   adb -s emulator-5554 install -r app/build/outputs/apk/ci/e2e/app-ci-e2e.apk
   ```

   The `e2e` build type exists only for the `ci` flavor, which has
   WalletConnect (the F-Droid flavors do not). It uses the debug backends and
   keys, installs as `io.horizontalsystems.bankwallet.appcenter.e2e` next to
   the dev builds, and shows as "UW E2E" in the launcher. Behind
   `IAppConfigProvider.e2eBuild` it differs from the dev build in a few ways:

   - Nothing can be broadcast: every send path throws
     `BroadcastDisabledException` (`BroadcastGuardTest` covers it).
   - Test tags are exposed as resource ids, which is how Maestro finds them.
   - No rate-app dialog, donation sheet or rooted-device warning.
   - Screenshots are allowed on pages that block them otherwise, so failure
     screenshots are not black.

## The flows

The flows are shared with the iOS app and live in the `flows/` folder of
the `main` branch of
[horizontalsystems/appkb](https://github.com/horizontalsystems/appkb/tree/main/flows).
How to write one, the kinds of tests, tags, platform branches, secrets and
the dust wallet are in its
[README](https://github.com/horizontalsystems/appkb/blob/main/flows/README.md).
This folder holds only what runs them on Android.

New feature tests are written into appkb by a job on the test station as
soon as the feature's pull request is merged, from its test tags and its
description. The station
runs the tip of the `main` branch.

`fetch-flows.sh` checks the flows out into `maestro/appkb/` (gitignored),
with your git credentials for appkb:

```bash
maestro/scripts/fetch-flows.sh            # the tip of main
maestro/scripts/fetch-flows.sh my-branch  # another appkb branch
```

To change a flow by hand, start a branch from `main` in that clone, edit
`maestro/appkb/flows/`, run it (below), and open a pull request in appkb.
`fetch-flows.sh` refuses to overwrite uncommitted edits in the clone.

## Running tests

Flows default to the e2e app id, `io.horizontalsystems.bankwallet.appcenter.e2e`.
Pass `-e APP_ID=...` only to run them against another build. Pass the emulator
with `--device` when a phone is also connected.

Run the journey (its steps run in order on one emulator):

```bash
maestro --device emulator-5554 test maestro/appkb/flows/journey
```

Run one feature's tests, or all feature and on-chain tests:

```bash
maestro --device emulator-5554 test maestro/appkb/flows/features/<feature>
maestro --device emulator-5554 test maestro/appkb/flows
```

The flows' `config.yaml` covers `features/` and `onchain/` only. The journey
has its own `journey/config.yaml` and always runs on its own.

Split feature tests across several emulators:

```bash
maestro --device "emulator-5554,emulator-5556" test --shard-split 2 maestro/appkb/flows
```

## Behaviour notes

Checked on Maestro 2.11.0 with the API 35 image:

- **Clearing app state:** `launchApp: { clearState: true }` is enough between
  tests. Creating a wallet, clearing state and restoring another works without
  a keystore error.
- **Keystore authentication window:** the app's keystore key needs a device
  unlock within the last 24 hours. Emulators are booted fresh for each run and
  unlocked by `device-prepare.sh`, so the window is always fresh. An emulator
  kept running for more than a day needs `device-prepare.sh` again.
- **Animations:** with the animation scales at 0, the balance screen's sync
  spinners do not hold Maestro up; taps take 1–2.5 s including Maestro's
  settle wait.
- **Rooted-device warning:** not shown on this image. The build tags are
  `dev-keys`, and the app cannot see `/system/xbin/su`.
- **Environment variables:** variables prefixed `MAESTRO_` are available in
  flows without `-e`, for example `${MAESTRO_E2E_DUST_MNEMONIC}`. Use this for
  secrets, so they stay out of process arguments.
- **Logged input:** text typed with `inputText` is written in plain text to
  `commands.json` and `logs/maestro.log` in the test output. E2E builds also
  allow screenshots everywhere, so a failure screenshot of the restore screen
  shows the phrase typed into it. Output from flows that type a secret needs
  those files redacted, and its screenshots checked, before it is shared.
- **Device logs:** Maestro saves `logs/device-logcat.txt` with each test's
  output, so crashes come with a stack trace.
- **Reports:** `--format` takes one of `JUNIT`, `HTML` or `HTML-DETAILED` per
  run.
- **Journey order:** `executionOrder.flowsOrder` in a folder's `config.yaml`
  works and `continueOnFailure: false` stops at the first failed step. Entries
  match flow names, which are the file names unless a flow sets `name:`. An
  entry that matches nothing is ignored silently, and the flow then runs in
  arbitrary order.
- **Subflow parameters:** a flow's own `env:` block overrides the values its
  caller passes through `runFlow`. A subflow that takes parameters sets its
  defaults in JavaScript instead, such as `${CHAINS || 'bitcoin'}`.
- **"No Internet" on a working emulator:** the app trusts Android's own
  check of the default network. On these images the emulator's Wi-Fi fails
  that check (the system probe's DNS lookups fail) while the mobile network
  passes, and Wi-Fi wins as the default. `device-prepare.sh` therefore turns
  Wi-Fi off and waits until the default network is validated. On an emulator
  you prepared by hand, run `adb shell svc wifi disable` when send or swap
  show "No Internet".
- **Android Studio and adb:** with Studio open, it restarts the adb server
  while new emulators boot, and adb commands fail for half a minute
  ("protocol fault"). The scripts retry through it.

## Scripts

| Script | What it does |
|---|---|
| `fetch-flows.sh` | Checks the shared flows out of appkb into `maestro/appkb/` |
| `device-prepare.sh` | Screen lock, animations, locale, unlock, network and clock checks, APK install; the test station runs it too |
| `avd-create.sh` | Creates the local emulator (Apple Silicon, arm64 image) |
| `maestro.sh` | `maestro` with the settings from `.env.e2e` loaded, for local runs |
| `load-config.sh` | Loads `.env.e2e`; sourced by the other scripts |

## Test tag check

The Check Test Tags workflow fails a pull request when a flow on appkb's
`main` branch selects an `id:` that `TestTags` does not declare, outside
steps that run only on iOS. It reads appkb with the `APPKB_TOKEN` repository
secret, a read-only token for that repository. Pull requests from forks get
no secrets and skip the check. To run it locally, after `fetch-flows.sh`:

```bash
python3 .github/scripts/check_test_tags.py
```

## Secrets

Local runs read their settings from `maestro/.env.e2e`. It is gitignored;
`.env.example` lists every setting with a comment:

```bash
cp maestro/.env.example maestro/.env.e2e
chmod 600 maestro/.env.e2e
```

| Setting | Used for |
|---|---|
| `MAESTRO_E2E_DUST_MNEMONIC` | The on-chain tests |
| `E2E_DEVICE_PIN` | Emulator screen lock PIN (default `1111`) |

Run flows with the wrapper so they get the file's values:

```bash
maestro/scripts/maestro.sh --device emulator-5554 test maestro/appkb/flows/onchain
```

- A variable already set in the environment wins over the file.
- Never pass the phrase with `-e`: it would show in the process list. Output
  from a hand-run `secret` flow contains the phrase; do not share it as it is.
