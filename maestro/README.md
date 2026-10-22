# Maestro UI Tests

## Setup

Tests run against the `ci` flavor, the dev build the team uses, on an arm64
emulator.

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

   The `e2e` build type exists only for the `ci` flavor, which has WalletConnect
   (the F-Droid flavors do not). It uses the
   debug backends and keys, installs as
   `io.horizontalsystems.bankwallet.appcenter.e2e` next to the dev builds, and
   shows as "UW E2E" in the launcher.

## Running tests

Flows default to the e2e app id, `io.horizontalsystems.bankwallet.appcenter.e2e`.
Pass `-e APP_ID=...` only to run them against another build. Pass the emulator
with `--device` when a phone is also connected.

Run the journey (its steps run in order on one emulator):

```bash
maestro --device emulator-5554 test maestro/journey
```

Run one feature's tests, or all feature and on-chain tests:

```bash
maestro --device emulator-5554 test maestro/features/<feature>
maestro --device emulator-5554 test maestro/
```

`maestro/config.yaml` covers `features/` and `onchain/` only. The journey has
its own `journey/config.yaml` and always runs on its own.

Split feature tests across several emulators:

```bash
maestro --device "emulator-5554,emulator-5556" test --shard-split 2 maestro/
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
  `commands.json` and `logs/maestro.log` in the test output. Flows that type a
  secret need those files redacted before they are shared.
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
  ("protocol fault"). The scripts retry through it; the runner Mac has no
  Studio.

## Runner

`scripts/run-suite.sh` runs the suite the way the runner Mac does: lock,
fresh checkout, build, read-only emulators, journey on one, feature and
on-chain tests split across the others, results in `~/e2e/runs/`, Telegram
report. Try it on your machine against your working copy:

```bash
maestro/scripts/run-suite.sh --local . --tags journey
maestro/scripts/run-suite.sh --local . --tags feature --shards 2
```

It boots its own read-only instances of `e2e_api35` on ports from 5580, so
an `e2e_api35` you started yourself must be stopped first; other emulators
can stay open.

| Script | What it does |
|---|---|
| `setup-runner.sh` | Installs the toolchain, the AVD, the scripts in `~/e2e/bin` and the launchd agents on a fresh `ci` user |
| `run-suite.sh` | One suite run; `--branch`, `--tags`, `--shards`, `--local`, `--trigger` |
| `poll-commits.sh` | Every 10 minutes (launchd): runs the journey when the newest `version/*` has a new head |
| `emulators-up.sh` / `emulators-down.sh` | Start and stop read-only instances |
| `device-prepare.sh` | Screen lock, animations, locale, unlock, network and clock checks, APK install |
| `collect-artifacts.sh` | Removes the dust wallet phrase and on-chain screenshots from a run directory |
| `maestro.sh` | `maestro` with the settings from `.env.e2e` loaded |
| `load-config.sh` | Loads `.env.e2e`; sourced by the other scripts |
| `notify-telegram.sh` | Posts failures, setup errors, flaky passes and the nightly heartbeat; prints them when no bot token is stored |
| `junit.py` | Reads Maestro's JUnit reports: failed flows to retry, the summary, the history file |
| `maintenance.sh` | Daily (launchd): silence alarm, pruning, disk check, Sunday flaky digest |
| `flaky-report.py` | 14-day flake rate per flow from `~/e2e/history/results.jsonl` |

Each run directory holds `journey.xml` and `flows.xml` (JUnit), the Maestro
output with failure screenshots, `logcat-<serial>.txt`, `build.log`,
`summary.txt` and `meta.json`, plus `*-retry.*` when something was retried.
`~/e2e/runs/latest` points to the newest.

### Failures, retries and flaky tests

- A failed test gets one retry in the same run. A failed journey reruns from
  step 01, because its steps depend on each other; feature and on-chain tests
  rerun one by one. A test that passes on the retry counts as passing and is
  reported as **flaky**.
- Every result goes to `~/e2e/history/results.jsonl`. The Sunday digest lists
  flows that flaked 3 times or in more than 10% of runs in 14 days.
- Such a feature or on-chain test gets the `quarantine` tag and an issue
  labelled `e2e-flaky`. `config.yaml` leaves quarantined tests out; fix or
  delete them within 2 weeks.
- Journey steps are never quarantined: a flaky step is fixed first, or moved
  into a feature test.
- A flaky test is fixed by waiting on a concrete element. Sleeps and extra
  retries do not count as a fix.

The launchd agents run the scripts from `~/e2e/bin`, not from the checkout:
`run-suite.sh` checks out other branches in its clone and would replace
itself mid-run. Rerun `setup-runner.sh` after changing a script.

## Secrets

The phrase of the team dust wallet, the Telegram bot and other local settings
go in `maestro/.env.e2e`. It is gitignored; `.env.example` lists every
setting with a comment. Create it once and fill it in:

```bash
cp maestro/.env.example maestro/.env.e2e
chmod 600 maestro/.env.e2e
```

| Setting | Used for |
|---|---|
| `MAESTRO_E2E_DUST_MNEMONIC` | The on-chain tests (`onchain/`, tagged `secret`) |
| `TELEGRAM_TOKEN`, `TELEGRAM_CHAT_ID` | Run reports; without them the message is printed |
| `E2E_REPO` | Repository the runner clones and polls; a local path works |
| `E2E_HOME` | Results, lock and history (default `~/e2e`) |
| `E2E_DEVICE_PIN` | Emulator screen lock PIN (default `1111`) |

Every script loads the file itself. To run flows by hand with it, use the
wrapper instead of `maestro`:

```bash
maestro/scripts/maestro.sh --device emulator-5554 test maestro/onchain
```

- On the runner Mac the file is `~/e2e/.env.e2e`, next to the scripts in
  `~/e2e/bin`; `setup-runner.sh` creates it empty.
- A variable already set in the environment wins over the file.
- Instead of the file, the phrase and the bot can live in the Keychain, which
  the scripts check when a value is empty:
  `security add-generic-password -s uw-e2e -a dust_mnemonic -w` (and
  `telegram_token`, `telegram_chat_id`).
- Never pass the phrase with `-e`: it would show in the process list. Maestro
  writes typed text to `commands.json` and `maestro.log`; `run-suite.sh`
  removes the phrase from its run directories, but output from a hand-run
  `secret` flow must not be shared as it is.

## Topping up the dust wallet

The dust wallet holds a few dollars of BTC, ETH and TRX and is treated as
burnable. The send tests fill in a send to the confirmation screen and never
broadcast, so its balance only drops through dust attacks or a bug.

- **Who:** the suite owner.
- **When:** `onchain/dust_balance_check.yaml` runs first in every on-chain run
  and fails with "Dust wallet below threshold for <COIN>, top up". The
  thresholds are in that file, about 2 × (send amount + typical fee).
- **How:** send from the team treasury to the dust wallet's first receive
  address for that coin. TRX needs a little extra: the TRX test's recipient
  is unactivated, so its fee estimate includes activation.

## Test structure

See [`docs/testing/maestro-integration-plan.md`](../docs/testing/maestro-integration-plan.md)
for the full plan.

There are three kinds of tests:

- **The journey**: one test made of ordered steps that walk the main user path
  on a single app install, the way a real user would.
- **Feature tests**: small independent tests, one or more per feature, added by
  the developer who builds the feature.
- **On-chain tests**: the few tests that need a funded wallet or live chain
  history. They run nightly only.

```
maestro/
├── journey/            # the main user path, ordered steps
├── features/           # one folder per feature, independent tests
│   ├── _template.yaml
│   └── <feature>/<scenario>.yaml
├── onchain/            # funded wallet or live history, nightly
└── subflows/
    ├── setup/wallet_ready.yaml   # the start of every feature test
    ├── setup/watch_wallet_ready.yaml  # fresh install with a watch-only wallet (ADDRESS, CHAIN)
    ├── onboarding/               # intro, terms
    ├── wallet/                   # create, restore (MNEMONIC, CHAINS), watch address, wait for sync
    ├── send/                     # send_to_confirm (COIN, AMOUNT)
    └── pin/                      # set_pin, enter_pin (PIN)
```

### The journey

Each step is its own file and the steps run in a fixed order on the same app
state. Only the first step clears the app; every later step continues from
where the previous one stopped. Because each step is a separate file, the
report names the step that failed.

```
journey/
├── config.yaml              # run order, stop at the first failure
├── 01_onboarding.yaml       # fresh install, intro, terms
├── 02_create_wallet.yaml    # new wallet, "Backup required" shown
├── 03_backup.yaml           # reveal phrase, pass the quiz
├── 04_set_pin.yaml          # set PIN, lock from background, wrong PIN rejected
├── 05_enable_token.yaml     # Manage coins → search USDT → enable → shows on balance
├── 06_receive.yaml          # receive screen shows an address
├── 07_delete_wallet.yaml    # delete the created wallet
├── 08_restore_wallet.yaml   # restore from the public test phrase
├── 09_check_addresses.yaml  # exact BTC/ETH addresses match the test vector
├── 10_send_form.yaml        # send form: address, amount, "insufficient balance"
├── 11_history.yaml          # transactions screen opens
├── 12_swap.yaml             # swap tab, pick a pair, quote or error appears
└── 13_settings.yaml         # App Status, base currency
```

`journey/config.yaml` lists the steps by file name in `flowsOrder`. A new
step needs an entry there, or it runs in arbitrary order.

A step continues from the screen the previous one left, so it starts by
choosing a tab and ends back on a main tab:

```yaml
appId: ${APP_ID || 'io.horizontalsystems.bankwallet.appcenter.e2e'}
tags: [journey]
---
- tapOn:
    id: tab_balance
- tapOn:
    id: balance_manage_coins
- tapOn:
    id: search_bar
- inputText: "USDT"
- tapOn:
    id: manage_coin_USDT_ethereum
# ... back to the balance screen
- scrollUntilVisible:
    element:
      id: balance_item_USDT
```

Rules for the journey:

- Use only the public test phrase, never the funded team wallet, so the
  journey can always be recorded and screenshotted. A real send up to the
  confirmation screen needs a balance and is a separate nightly test.
- Only step 01 uses `clearState`. A step must not reset state the next step
  relies on.
- When a step fails, the remaining steps are skipped for that run. Fix the
  first failure first.
- Keep the whole journey under 10 minutes (about 4.5 today). A step that grows
  long moves into a feature test.

### Feature tests

Each feature test is independent and starts from a ready wallet in one line,
so it only contains the steps for its feature:

```yaml
appId: ${APP_ID || 'io.horizontalsystems.bankwallet.appcenter.e2e'}
name: "Contacts · add a contact from the send screen"
tags: [feature, contacts]
---
# Ready state: app cleared, onboarding passed, public test wallet restored with BTC
- runFlow:
    file: ../../subflows/setup/wallet_ready.yaml
    env:
      CHAINS: "bitcoin"

# ... the feature's own steps and assertions
```

`features/wallets/rename_wallet.yaml` is a complete example.

Rules for feature tests:

- A pull request that adds or changes a user-facing feature adds or updates a
  test under `features/<feature>/`. Start from `features/_template.yaml`.
- Always start with `subflows/setup/wallet_ready.yaml`, unless the test is
  about onboarding or restoring itself (`features/restore/`). Never depend on
  another test or on the order tests run in.
- Keep each test under 2 minutes.
- Select elements by test tag (`id:`). New screens get their test tags in the
  same pull request.
- Run it locally before opening the pull request:

  ```bash
  maestro test maestro/features/<feature>
  ```

- When a feature becomes part of the main user path, add a step for it to the
  journey and delete the feature test.

### When tests run

| Tests | Tag | When |
|---|---|---|
| Journey (about 4.5 min) | `journey` | Every new commit on `version/*`, and nightly |
| Feature tests | `feature` | Nightly, and locally by the developer while working on the feature |
| Real send to confirm (funded wallet), history with known transactions | `onchain` | Nightly |
