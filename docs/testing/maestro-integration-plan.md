# Maestro UI Test Integration Plan

A scenario-based UI test suite that runs unattended on Android emulators on a
dedicated Apple Silicon Mac, triggered by new commits on `version/*` and
nightly, with JUnit and HTML reports, failure artifacts and Telegram
notifications. Everything runs locally on the Mac; there is no GitHub Actions
integration for now.

## Decisions

| Topic | Decision |
|---|---|
| Framework | Maestro YAML flows for the regression suite; LLM tools only as an optional later phase |
| Target build | F-Droid CI flavor (`fdroidCi`) with a dedicated `e2e` build type |
| Runner | Dedicated Apple Silicon Mac, native arm64 emulators, scheduled by launchd. No GitHub Actions integration for now |
| Triggers | New commits on the newest `version/*` branch (polled), nightly, manual |
| On-chain | No broadcasts. Send flows stop at the confirmation screen. Testnet support is deferred |
| Chains | Bitcoin, Ethereum and the light chains. Zcash, Monero and Zano are out of scope for now |
| Notifications | Existing Telegram bot and chat, called directly from the Mac through the Bot API |
| Cloud devices | None for now |

## What exists today

The plan builds on these. It does not replace them.

- `maestro/`: 4 sub-flows (`pass_intro`, `accept_terms`, `create_standard_wallet`,
  `restore_wallet_from_phrase`) and 7 flows (intro, create, restore, BTC/ETH
  receive address vectors, BTC send to confirm, backup quiz, App Status). They
  were written against `ciDebug` and select everything by English text.
- `.github/workflows/maestro_tests.yml`: manual only. It has never run. It
  cannot pass as written, because the emulator has no screen lock (see below)
  and it runs an ARM-only APK on an x86_64 emulator. This plan leaves it
  untouched; whether to fix or delete it is decided when GitHub integration is
  revisited.
- 2 Compose instrumented tests in `app/src/androidTest/`, about 65 JVM unit test
  files. None of them run in CI.

## Constraints from the codebase

These shape several steps below. Each is referenced by its ID.

| ID | Constraint | Source |
|---|---|---|
| C1 | The app refuses to run without a device screen lock and calls `resetApp("NoSystemLock")` | `MainActivityViewModel.kt:88`, `Nav3.kt:273`, `KeyStoreViewModel.kt:33` |
| C2 | The keystore key needs device authentication within 24h (`AUTH_DURATION_SEC`) | `KeyStoreManager.kt:111-122` |
| C3 | Rooted-device warning on `google_apis` images (`test-keys` and `su`) | `RootUtil.kt`, `MainPage.kt:77` |
| C4 | FLAG_SECURE on 17 pages and the unlock overlay, so failure screenshots and recordings are black there | `Nav3.kt:347-363`, `HSPage(screenshotEnabled = false)` |
| C5 | Native libraries are ARM-only (`abiFilters`), so x86_64 emulators run under translation | `app/build.gradle.kts:51-53` |
| C6 | No `testTag` anywhere, and `testTagsAsResourceId` is not enabled | whole codebase |
| C7 | Rate-app dialog after 5 launches + 10 s; donation sheet (F-Droid only) after a send adds a recent contact | `RateAppManager.kt:28-30`, `DonationShowManager.kt`, `MainViewModel.kt:177` |
| C8 | Infinite Compose animations (sync spinners) can delay Maestro's idle detection | `HSCircularProgressIndicator.kt`, `BalanceCard2.kt` |
| C9 | All chains are hard-wired to mainnet; ethereum-kit has no Sepolia | `BitcoinAdapter.kt:108`, `EvmKitManager.kt:317` |
| C10 | The F-Droid flavor allows every paid action except PrioritySupport, ships no WalletConnect and no passkeys | `SubscriptionServiceFDroid.kt:18`, `app/build.gradle.kts` `afterEvaluate` |
| C11 | Bottom tabs are Market, Balance, Swap, Settings. Transactions is not a tab | `MainModule.kt:110` |
| C12 | `send_btc.yaml` uses the public "abandon" wallet, which any bot can sweep | `maestro/wallet/send_btc.yaml` |

---

## Phase 1: Local proof of concept

Goal: prove that the existing flows run headless on a native arm64 emulator
against the F-Droid build, and settle the open technical questions before
investing in the rest.

### 1.1 Tooling and emulator

- Install Maestro, pinned to one version. Record that version in
  `maestro/README.md`; every machine uses it.
- Install the system image `system-images;android-35;google_apis;arm64-v8a` and
  create an AVD `e2e_api35` (Pixel 6 profile, 4 GB RAM, 8 GB data partition).
- Prepare the device once:
  - `adb shell locksettings set-pin 1111` (C1)
  - set `window_animation_scale`, `transition_animation_scale` and
    `animator_duration_scale` to 0
  - `svc power stayon true`
  - locale en-US, time zone UTC

**Files:** `maestro/README.md`.
**Done when:** `maestro --version` matches the pinned version, and the AVD boots
headless (`-no-window`) with a screen lock set.
**Effort:** 0.5 day.

### 1.2 Run 2 flows against the F-Droid debug build

- Build with `./gradlew :app:assembleFdroidCiDebug`. The applicationId is
  `io.horizontalsystems.bankwallet.fdroidci.dev`.
- Run `onboarding/intro.yaml` and `onboarding/create_wallet_standard.yaml` with
  `-e APP_ID=io.horizontalsystems.bankwallet.fdroidci.dev`.
- Fix any differences between the F-Droid and `ci` flavors in the sub-flows
  (C10). For example, the import options differ because F-Droid has no passkey
  entry.

**Files:** `maestro/subflows/*.yaml`.
**Done when:** both flows pass 5 times in a row on a headless emulator.
**Effort:** 0.5 day.

### 1.3 Settle the open technical questions

Record each answer in `maestro/README.md`:

1. **Keystore and `clearState`:** does `clearState: true` (`pm clear`) wipe the
   app's keystore key, or does the next launch hit `KeyInvalidated`? If it
   does not wipe it, flows have to reinstall the app instead (slower).
2. **Keyguard after snapshot load:** does unlocking the keyguard after a
   snapshot load reset the 24h keystore window (C2)?
3. **Animations:** do the infinite animations stop at
   `animator_duration_scale 0`, or do some screens still hold Maestro's idle
   wait (C8)?
4. **Parallel runs:** what is the exact Maestro CLI syntax for sharding across
   several emulators (`--shard-split`, device selection) in the pinned version?
5. **Secret env vars:** are `MAESTRO_*` env variables read without `-e`, so
   secrets stay out of process arguments?
6. **Logged input:** does Maestro's debug output record `inputText` values?
   This decides the redaction step in 6.2.

**Done when:** all six have a written answer.
**Effort:** 1 day.

---

## Phase 2: App-side preparation

### 2.1 Dedicated `e2e` build type

Add a build type rather than a flavor. That way it combines with the
`distribution` flavors unchanged and only `fdroidCiE2e` is enabled.

```kotlin
// app/build.gradle.kts
defaultConfig { buildConfigField("boolean", "E2E_BUILD", "false") }

buildTypes {
    create("e2e") {
        initWith(getByName("debug"))
        applicationIdSuffix = ".e2e"
        matchingFallbacks += "debug"
        buildConfigField("boolean", "E2E_BUILD", "true")
    }
}

androidComponents {
    beforeVariants(selector().withBuildType("e2e")) { variant ->
        variant.enable = variant.productFlavors.any { it.second == "fdroidCi" }
    }
}
```

- The variant is `fdroidCiE2e`, the applicationId
  `io.horizontalsystems.bankwallet.fdroidci.e2e`, the APK
  `app/build/outputs/apk/fdroidCi/e2e/app-fdroidCi-e2e.apk`. It installs side by
  side with the regular dev builds.
- It inherits debug's dev backends and keys. LeakCanary is not included
  (`debugImplementation` only), which is good: no heap-dump notifications.
- Check that `fdroidCiImplementation(project(":subscriptions-fdroid"))` resolves
  for the new build type.
- `:walletkit` and the chain modules resolve their `debug` variant through
  `matchingFallbacks`, so `BuildConfig.DEBUG` is true there and the Settings
  debug section (`MainSettingsScreen.kt:453`) is available.
- Optional: `app/src/fdroidCiE2e/res/values/strings.xml` with an app name of
  "UW E2E", so the build is easy to tell apart on a device.

**Files:** `app/build.gradle.kts`, optionally `app/src/fdroidCiE2e/`.
**Done when:** `./gradlew :app:assembleFdroidCiE2e` produces the APK, the build
type shows no other variants, and `assembleFdroidRelease` output is unchanged
(same APK contents as before).
**Effort:** 0.5 day.

### 2.2 E2E hooks behind one flag

Expose the flag the same way `fdroidBuild` already is: add
`val e2eBuild: Boolean` to `IAppConfigProvider` and implement it in
`AppConfigProvider` as `BuildConfig.E2E_BUILD`. Every hook reads
`App.appConfigProvider.e2eBuild`, and nothing else turns them on.

| Hook | Change | File |
|---|---|---|
| Rooted warning (C3) | Do not show it when `e2eBuild` | `modules/rooteddevice/RootedDeviceViewModel.kt:15` |
| FLAG_SECURE (C4) | Never add the flag when `e2eBuild`, so failure screenshots are readable | `modules/nav3/Nav3.kt` `ToggleScreenshot` |
| Rate-app (C7) | Never show it when `e2eBuild` | `core/managers/RateAppManager.kt` |
| Donation sheet (C7) | `shouldShow()` returns false when `e2eBuild` | `core/managers/DonationShowManager.kt` |
| Broadcast guard (C12) | The final send fails with "Broadcasting is disabled in E2E builds" when `e2eBuild`. A safety net in case a flow ever taps Send with a real wallet | the single point where a signed transaction is handed to the adapter (to be located; if no single point exists, guard each `SendTransactionService`) |

What deliberately stays as in production:

- **Device screen lock (C1, C2):** the emulator gets a real PIN instead of a
  bypass, so the real keystore path, `setUserAuthenticationRequired`, is what
  gets tested. The runner unlocks the keyguard before every run (5.2).
- **Biometrics:** opt-in in the app, and no fingerprint is enrolled on the
  emulator, so no prompt ever appears. A future biometric flow can enroll one
  and use `adb emu finger touch 1`.
- **Animations:** disabled at the device level (1.1, 5.2) with no app change,
  unless 1.3 shows a screen that still blocks.
- **Testnet defaults:** deferred (decision C). The flag can later select
  testnet networks for Tron Nile, TON, XRP and NEAR, whose kits already support
  them. EVM first needs Sepolia in ethereum-kit, and Bitcoin only has testnet3.

**Files:** `walletkit/.../core/providers/IAppConfigProvider.kt`,
`app/.../core/providers/AppConfigProvider.kt`, and the files in the table.
**Done when:**
- on the e2e APK, a `google_apis` emulator shows no rooted warning, a failure
  screenshot on `RecoveryPhrasePage` is not black, and the rate-app dialog never
  appears after 10 launches
- tapping the final Send button shows the broadcast-disabled error
- on `fdroidCiDebug`, all four behaviours are unchanged
**Effort:** 1.5 days.

### 2.3 Stable selectors

**Exposing tags.** Enable `testTagsAsResourceId` once at the root, in
`MainActivity.setContent` around `Nav3(EntryPage)`, so Maestro can select with
`id:`. It costs nothing and is harmless in release, so it needs no flag.

`ModalBottomSheet` and dialogs render in their own windows and do not inherit
this setting. Apply it again inside:

- `uiv3/components/bottomsheet/BottomSheet.kt`
- `ui/extensions/HSBottomSheet.kt`
- the other `ModalBottomSheet` call site
- any `Dialog` used by the flows (rate-app, PIN, `BiometricPromptDialog`)

**Tag conventions.**
- `snake_case`, `<screen>_<element>`.
- Dynamic suffixes only where a flow needs to pick one item: coin code,
  blockchain uid, word index.
- Constants live in one object, `walletkit/.../uiv3/TestTags.kt`, so a rename
  shows up in a code search.
- Pass tags through the existing `modifier` parameters. Where a shared `uiv3`
  component has no `modifier`, add one rather than wrapping it.

**Screens and elements to tag.** Paths are relative to
`walletkit/src/main/java/io/horizontalsystems/walletkit/modules/`.

| Screen | File | Tags |
|---|---|---|
| Intro | `intro/IntroScreen.kt` | `intro_next`, `intro_skip` |
| Bottom tabs | `main/MainPage.kt` | `tab_market`, `tab_balance`, `tab_swap`, `tab_settings` |
| No-account balance | `balance/ui/BalanceNoAccount.kt` | `onboarding_new_wallet`, `onboarding_existing_wallet` |
| Terms | `settings/terms/TermsPage.kt` | `terms_check_<n>` (0-based), `terms_next` |
| Create wallet | `createaccount/CreateAccountPage.kt`, `CreateAccountStandardPage.kt` | `create_type_standard`, `create_name_input`, `create_submit` |
| Import options | `importwallet/ImportWalletPage.kt` | `import_recovery_phrase`, `import_watch_address`, `import_backup_file` |
| Phrase input | `restoreaccount/restoremnemonic/RestorePhraseScreen.kt` | `restore_phrase_input`, `restore_next`, `restore_error` |
| Restore chains | `restoreaccount/restoreblockchains/RestoreBlockchains.kt` | `restore_chain_<blockchainUid>`, `restore_submit` |
| Watch address | `watchaddress/WatchAddressPage.kt` | `watch_address_input`, `watch_submit` |
| Balance | `balance/ui/BalanceCard2.kt`, `BalanceItems.kt`, `BalanceForAccount.kt` | `balance_total`, `balance_receive`, `balance_send`, `balance_swap`, `balance_transactions`, `balance_item_<coinCode>`, `balance_item_<coinCode>_amount`, `balance_sync_indicator` |
| Token page | `balance/token/TokenBalancePage.kt` | `token_balance`, `token_receive`, `token_send`, `token_swap`, `token_sync_state` |
| Receive | `receive/ui/ReceiveAddressScreen.kt` | `receive_address`, `receive_network`, `receive_format`, `receive_copy`, `receive_share` |
| Send form | `send/v2/SendV2Screen.kt`, `SendInputCell.kt` | `send_recipient_row`, `send_amount_input`, `send_max`, `send_next`, `send_error` |
| Send address | `send/v2/SendAddressPage.kt` | `send_address_input`, `send_address_next`, `send_address_error` |
| Send confirm | `send/v2/SendV2ConfirmPage.kt` | `confirm_amount`, `confirm_recipient`, `confirm_network_fee`, `confirm_send` |
| Transactions | `transactions/TransactionsScreen.kt` | `tx_list`, `tx_item_<txHashPrefix8>`, `tx_filter_<type>`, `tx_empty` |
| Transaction info | transaction info page (open from `tx_item_*`) | `txinfo_status`, `txinfo_amount`, `txinfo_hash` |
| Swap | `multiswap/SwapPage.kt` | `swap_token_in`, `swap_token_out`, `swap_amount_in`, `swap_switch`, `swap_provider_row`, `swap_next`, `swap_quote_error` |
| Swap providers | `multiswap/SwapSelectProviderPage.kt` | `swap_provider_item_<providerId>` |
| Token search | `uiv3/components/bottom/BottomSearchBar.kt` | `search_input`, `search_clear` |
| Settings | `settings/main/MainSettingsScreen.kt` | `settings_manage_wallets`, `settings_security`, `settings_about`, `settings_debug_*` |
| Security | `settings/security/SecuritySettingsPage.kt` | `security_passcode`, `security_autolock`, `security_biometrics`, `security_duress` |
| PIN pad (set, confirm, unlock) | `pin/ui/PinNumpad.kt`, `pin/ui/PinUnlockView.kt`, `pin/SetPinPage.kt` | `pin_key_<0-9>`, `pin_key_delete`, `pin_error`, `pin_title` |
| Manage wallets | `manageaccounts/ManageAccountsPage.kt`, `manageaccount/ManageAccountPage.kt` | `account_row_<index>`, `account_backup_required`, `account_manual_backup` |
| Backup phrase | `manageaccount/backupkey/BackupKeyPage.kt`, `recoveryphrase/RecoveryPhrasePage.kt` | `backup_reveal`, `backup_word_<n>`, `backup_verify` |
| Backup quiz | `manageaccount/backupconfirmkey/BackupConfirmKeyPage.kt` | `quiz_position_<0/1>`, `quiz_option_<word>` |
| App Status | `settings/appstatus/` | `appstatus_app_info` |

That is about 75 tags. The two most fragile selectors in the existing flows are
replaced first:

- `accept_terms.yaml`: 9 sentence regexes become `terms_check_<n>`.
- `backup_recovery_phrase.yaml`: `[a-z]+` by index becomes `backup_word_<n>`.

**Done when:**
- Maestro Studio or `maestro hierarchy` shows every tag in the table as a
  `resource-id`, including the ones inside bottom sheets
- the existing flows use `id:` for every tap, and text only for assertions on
  user-visible content (addresses, amounts)
- `./gradlew :walletkit:lint` passes
**Effort:** 3–4 days.

### 2.4 Keep selectors alive

- Add a short section on UI tests to `AGENTS.md`:
  - the test tag naming convention, the `TestTags` object, and "do not remove
    or rename a tag without updating `maestro/`"
  - the feature test rules from 3.3, with a pointer to `maestro/README.md`
- Add a pull request checklist line: "User-facing change → feature test added
  or updated, and the journey run locally if a journey screen changed."

**Done when:** `AGENTS.md` documents the tag convention and the feature test
rules.
**Effort:** 0.25 day.

---

## Phase 3: Core flow suite

The suite has three kinds of tests. `maestro/README.md` ("Test structure")
is the reference for developers; this phase is how it gets built.

- **The journey:** one test made of ordered steps that walk the main user path
  on a single app install, the way a real user would.
- **Feature tests:** small independent tests, added by the developer who builds
  a feature.
- **On-chain tests:** the few tests that need the funded team wallet or live
  chain history. They run nightly only.

### 3.1 Folder structure and tags

```
maestro/
├── config.yaml                  # flow globs, excludeTags: [quarantine]
├── README.md                    # pinned version, how to run, env variables, test structure
├── .env.example                 # variable names only, never values
├── journey/
│   ├── config.yaml              # flowsOrder, continueOnFailure: false
│   └── 01_onboarding.yaml … 13_settings.yaml
├── features/
│   ├── _template.yaml
│   └── <feature>/<scenario>.yaml
├── onchain/
│   ├── send/          btc_to_confirm, eth_to_confirm, trx_to_confirm
│   ├── history/       watch_wallet_history, tx_details
│   └── dust_balance_check.yaml
├── subflows/
│   ├── setup/
│   │   └── wallet_ready.yaml             # clear app, pass onboarding, restore the public vector; params: CHAINS
│   ├── onboarding/
│   │   ├── pass_intro.yaml
│   │   └── accept_terms.yaml
│   ├── wallet/
│   │   ├── create_standard_wallet.yaml
│   │   ├── restore_from_phrase.yaml      # params: MNEMONIC, CHAINS
│   │   ├── restore_watch_address.yaml    # params: ADDRESS
│   │   └── wait_for_sync.yaml            # params: COIN, TIMEOUT
│   ├── pin/
│   │   ├── set_pin.yaml                  # params: PIN (6 digits, PinModule.PIN_COUNT)
│   │   └── enter_pin.yaml                # params: PIN
│   └── nav/
│       ├── open_tab.yaml                 # params: TAB
│       └── open_token.yaml               # params: COIN
└── scripts/                              # runner scripts, see Phase 5
```

| Tag | Meaning | Runs on |
|---|---|---|
| `journey` | The ordered steps in `journey/` | new commits on `version/*`, nightly |
| `feature` | Everything in `features/`, plus a tag with the feature's name (e.g. `contacts`) | nightly, and locally by the developer |
| `onchain` | Everything in `onchain/`: needs the funded wallet or known chain history. Never broadcasts | nightly |
| `secret` | Types a secret from the environment; recording is disabled and logs are redacted (6.2) | combined with `onchain` |
| `quarantine` | A known-flaky feature or on-chain test, excluded from the main run (6.4). Never used on journey steps | nightly, non-blocking |

**Moving the existing flows:**

| Existing file | New place |
|---|---|
| `onboarding/intro.yaml` | `journey/01_onboarding.yaml`, including the relaunch check |
| `onboarding/create_wallet_standard.yaml` | `journey/02_create_wallet.yaml` |
| `wallet/backup_recovery_phrase.yaml` | `journey/03_backup.yaml` |
| `onboarding/restore_wallet_from_recovery_phrase.yaml` | `journey/08_restore_wallet.yaml` |
| `wallet/receive_address_btc.yaml`, `receive_address_eth.yaml` | `journey/09_check_addresses.yaml`, both vectors |
| `wallet/app_status.yaml` | `journey/13_settings.yaml` |
| `wallet/send_btc.yaml` | `onchain/send/btc_to_confirm.yaml`, switched to the team dust wallet (4.1) |

Then remove the `maestro/onboarding` and `maestro/wallet` folders.

**Files:** `maestro/**`.
**Done when:** `maestro test --include-tags journey maestro/journey` runs the 13
steps in order, `--include-tags feature` runs only `features/`, and the old
folders are gone.
**Effort:** 0.5 day.

### 3.2 The journey

Only step 01 clears the app; every later step continues from the state the
previous one left. The journey uses only the public test phrase and the
wallet it creates itself, never the funded team wallet. That way it can always
be recorded and screenshotted. "Vector" means an independently derived,
committed public test vector, the same pattern the existing receive flows use.

| Step | What it does and asserts |
|---|---|
| 01 onboarding | Fresh install, intro, terms via `terms_check_<n>`. Relaunch: intro not shown again |
| 02 create_wallet | `onboarding_new_wallet` → standard → create. Assert the balance screen, `balance_receive`, a $0 total, and "Backup Required" in Manage Wallets |
| 03 backup | Reveal the phrase, read `backup_word_<n>`, pass the quiz. Assert "Backup Required" is gone |
| 04 set_pin | Settings → Security → Passcode, set and confirm. Set auto-lock to Immediate, press Home, relaunch. A wrong PIN shows `pin_error` and stays locked (fewer attempts than the `LockoutManager` threshold); the right PIN opens the balance |
| 05 enable_token | Manage coins → search "USDT" → enable the Ethereum token. Assert `balance_item_USDT` on the balance screen |
| 06 receive | Token → Receive. Assert `receive_address` is not empty and `receive_network` matches |
| 07 delete_wallet | Manage Wallets → delete the created wallet. Assert the no-account screen |
| 08 restore_wallet | Existing wallet → recovery phrase → the public vector with BTC and ETH. Assert `balance_item_BTC` and `balance_item_ETH` |
| 09 check_addresses | Assert the exact BTC (BIP84) and ETH addresses and formats from the vectors |
| 10 send_form | BTC → Send → `send_address_input` with an invalid address shows `send_address_error`; a valid one, then an amount above the (zero) balance shows `send_error`. Never reaches confirmation |
| 11 history | Open transactions from the balance screen. Assert the list or `tx_empty` appears without errors |
| 12 swap | `tab_swap` → choose a pair via search → enter an amount. Assert that either a provider row or `swap_quote_error` appears within the timeout. Never go past the quote |
| 13 settings | App Status renders (`appstatus_app_info`); change the base currency and check the balance updates |

Rules:

- **Fixed order:** `journey/config.yaml` lists the steps in `flowsOrder` with
  `continueOnFailure: false`. When a step fails, the later steps are skipped
  for that run.
- **No state resets:** a step must not use `clearState` or reset anything the
  next step relies on.
- **Fallback for ordering:** if 1.3 shows `executionOrder` does not work with a
  separate config for this folder, the fallback is a single `journey.yaml` that
  calls the steps in order with `runFlow`. The failing step then shows by name
  in the log rather than as a separate report entry.
- **One emulator:** the journey cannot be split across emulators. It runs on
  one emulator, and other emulators take feature and on-chain tests in the
  nightly run.
- **Under 10 minutes:** so every commit can run it. A step that grows too long
  moves into a feature test.

**Done when:** the journey passes 10 consecutive runs on one emulator.
**Effort:** 3–4 days.

### 3.3 Feature tests

Each feature test is independent and starts from a ready wallet in one call,
so it only contains the steps for its feature:

```yaml
appId: ${APP_ID}
name: "Contacts · add a contact from the send screen"
tags: [feature, contacts]
---
- runFlow:
    file: ../../subflows/setup/wallet_ready.yaml
    env:
      CHAINS: "bitcoin"
# ... the feature's own steps and assertions
```

Tasks:

- Write `subflows/setup/wallet_ready.yaml`. It clears the app, passes
  onboarding, restores the public vector with the chains given in `CHAINS`, and
  ends on the balance screen.
- Write `features/_template.yaml` with the header, tags and the
  `wallet_ready` call.
- Write the first feature tests. These cover cases the journey leaves out, and
  show developers the pattern:
  - `restore/invalid_phrase`: a checksum-invalid phrase shows `restore_error`
    and blocks Next
  - `receive/tron_ton_sol_addresses`: Tron, TON and Solana address vectors
  - `send/payment_uri_prefill`: `openLink: bitcoin:<addr>?amount=…` pre-fills
    the send form (`MainViewModel.kt:543`)
  - `wallets/rename_wallet`: rename in Manage Wallets
- Add the developer rules to `AGENTS.md` (see 2.4).

Rules for developers (also in `maestro/README.md`):

- A pull request that adds or changes a user-facing feature adds or updates a
  test under `features/<feature>/`.
- Always start with `wallet_ready.yaml`. Never depend on another test or on the
  order tests run in.
- Keep each test under 2 minutes, and select elements by test tag (`id:`).
- Run it locally with `maestro test maestro/features/<feature>` before opening
  the pull request.
- When a feature becomes part of the main user path, add a journey step for it
  and delete the feature test.

**Done when:** each feature test passes when run alone and when all of them run
in a random order, and the template works for a new test without edits beyond
the feature's own steps.
**Effort:** 1.5–2 days.

### 3.4 On-chain tests

| Test | What it does and asserts |
|---|---|
| `send/*_to_confirm` | Restore the team dust wallet (4.1), `wait_for_sync`, then Send → address → amount → Next. Assert `confirm_amount`, `confirm_recipient` and a non-empty `confirm_network_fee`. **Never tap `confirm_send`**; the broadcast guard (2.2) also blocks it. Tagged `secret` |
| `history/watch_wallet_history` | `restore_watch_address` with a public address that has stable old history (4.1). Assert `tx_item_<hash>` for 2 known transactions. Assert specific known items, not counts, because spam and dust can arrive on any public address |
| `history/tx_details` | Open one known transaction; check amount and status |
| `dust_balance_check` | Runs first; fails with "Dust wallet below threshold for <COIN>, top up" (4.3) |

**Done when:** the on-chain tests pass 3 consecutive nightly-style runs.
**Effort:** 1.5–2 days.

### Guidelines for all tests

- Wait with `extendedWaitUntil` on a concrete element (`token_sync_state`,
  `confirm_network_fee`). Never with fixed sleeps.
- Keep chain sync to the coins a test needs. Restore enables only the chains it
  asserts on (the `CHAINS` parameter).
- Use English assertion text only for content (addresses, amounts). The
  emulator locale is fixed to en-US.

---

## Phase 4: Secrets and test data

### 4.1 Test wallets

| Wallet | Purpose | Where it lives |
|---|---|---|
| Public vectors (`token weekend gadget …`, plus new Tron, TON and Solana vectors) | Address derivation. Must stay unused on-chain | Committed in the flow `env:` blocks, as today |
| Team dust wallet | Send-to-confirm needs spendable balance. Holds dust on BTC, ETH and TRX (a few dollars total; treat it as burnable) | macOS Keychain on the runner Mac (and on each developer's machine), never in the repo |
| Watch addresses | History: public addresses with old, known transactions | Committed; history needs no secret |

Replace the "abandon" wallet in `send_btc.yaml` with the team dust wallet
(C12). Its public phrase means bots can sweep it at any time, and the flow then
fails at the amount step for reasons unrelated to the app.

### 4.2 Passing secrets

- Secrets live in the login Keychain of the `ci` user:
  `security add-generic-password -s uw-e2e -a dust_mnemonic -w`, and the same
  way for `telegram_token` and `telegram_chat_id`. `run-suite.sh` reads them
  with `security find-generic-password -s uw-e2e -a <name> -w` and exports
  `MAESTRO_E2E_DUST_MNEMONIC`; flows read `${MAESTRO_E2E_DUST_MNEMONIC}`.
  Using `-e` instead would put the phrase in process arguments, visible to
  `ps`. This depends on the answer to 1.3 question 5.
- No secret is written to a file on disk, and none is passed through launchd
  plists.
- Developers running flows on their own machine either use the same Keychain
  entries, or keep `maestro/.env.e2e`, which is gitignored (add it to
  `.gitignore`), and source it before running. `maestro/.env.example` lists the
  variable names only.
- Flows that use a secret are tagged `secret` (see 6.2 for what that changes).

### 4.3 Dust top-up procedure

Documented in `maestro/README.md`:

- **Who:** the suite owner.
- **Thresholds:** for example 2× (amount + typical fee) per coin.
- **How:** send from the team treasury to the dust wallet's first receive
  address.
- **Monitoring:** a nightly `onchain` flow, `onchain/dust_balance_check.yaml`,
  fails with "Dust wallet below threshold for <COIN>, top up" before the send
  flows run, so a low balance is not mistaken for an app bug.
- Faucet funding becomes relevant only when testnet support lands. Write a
  section then.

**Files:** `maestro/README.md`, `maestro/.env.example`, `.gitignore`, the
Keychain entries on the runner Mac.
**Done when:** no mnemonic other than the public vectors exists in the repo
(`git grep` for the secret's first word finds nothing), and the send flows pass
with the secret supplied only through the environment.
**Effort:** 1 day.

---

## Phase 5: Always-on runner

### 5.1 Machine

- Apple Silicon Mac (mini or Studio), at least 32 GB RAM. Each emulator takes
  about 4 GB RAM and 2 cores, so plan for 3 in parallel on 32 GB.
- A dedicated non-admin macOS user `ci`.
- Sleep, automatic macOS updates and App Nap disabled
  (`sudo pmset -a sleep 0 disablesleep 1`). FileVault on.
- Installed: Homebrew, Temurin JDK 17, Android command-line tools, platform
  tools, emulator, the system image from 1.1, and the pinned Maestro version.
  The versions are listed in `maestro/README.md` and installed by
  `maestro/scripts/setup-runner.sh`.

**Done when:** a fresh macOS user can be brought up by running one script.
**Effort:** 0.5 day.

### 5.2 Emulators and clean state

Scripts in `maestro/scripts/`:

- **`avd-create.sh`** creates `e2e_api35` and boots it once. It applies the
  device preparation from 1.1 (screen-lock PIN, animations off, stay awake,
  locale, time zone) and saves a snapshot `clean`.
- **`emulators-up.sh N`** starts N instances in parallel from the same AVD:

  ```
  emulator -avd e2e_api35 -read-only -snapshot clean -no-window -no-audio \
           -no-boot-anim -gpu swiftshader_indirect -port 5554|5556|5558
  ```

  `-read-only` lets the instances share one AVD and discards all changes on
  exit, so every run starts from the same state. The script waits for
  `sys.boot_completed`.
- **`device-prepare.sh <serial>`** runs after every boot:
  - wakes the device and unlocks the keyguard with the PIN; this resets the
    24h keystore window (C2)
  - re-applies the animation settings
  - uninstalls any previous e2e APK and installs the new one
  - checks the clock against the host
- **`emulators-down.sh`** runs `adb emu kill` for all instances.
  `run-suite.sh` calls it from an `EXIT` trap, so emulators are stopped even
  when a run fails or is interrupted.

Between flows, `clearState: true` is enough if 1.3 confirms the keystore key is
wiped. If not, the flow header uses `maestro/subflows/reinstall.yaml`, which
uninstalls and reinstalls through `runScript`/adb.

Weekly maintenance (launchd, Sunday): prune old Gradle caches and
`~/.maestro/tests`, check disk space, and recreate the AVD if the snapshot no
longer loads.

**Done when:** 3 emulators start and are prepared, the journey runs on one
while the feature tests are split across the other two, and a second run
directly after shows no state left over from the first.
**Effort:** 1.5 days.

### 5.3 Run script

`maestro/scripts/run-suite.sh [--branch <name>] [--tags <tags>] [--shards N]`
is the single entry point. launchd, developers and manual reruns all use it.

1. **Lock:** takes a lock with `mkdir ~/e2e/run.lock` and exits if the lock is
   already held. Only one suite runs at a time, and a nightly run never
   overlaps a commit run.
2. **Checkout:** a dedicated clone at `~/e2e/checkout`, separate from any
   working copy. It runs `git fetch origin`, resolves the branch (default: the
   newest `version/*`, via
   `git ls-remote --heads origin 'version/*' | sort -V | tail -1`) and checks
   out its head SHA detached, with `git clean -fdx` (Gradle caches live in
   `~/.gradle`, so they survive).
3. **Build:** `./gradlew :app:assembleFdroidCiE2e`.
4. **Devices:** `emulators-up.sh N`, then `device-prepare.sh` for each.
5. **Run:** reads the secrets from Keychain (4.2). The journey runs on the
   first emulator; feature and on-chain tests, when requested, are split across
   the remaining emulators at the same time. Failures are retried once (6.4).
6. **Report:** writes the results to `~/e2e/runs/<timestamp>-<sha8>/` (6.1),
   appends to the flake history and sends the Telegram message (6.3).
7. **Teardown:** the `EXIT` trap stops the emulators and releases the lock.

**Done when:** `run-suite.sh --tags journey` runs end to end from a clean Mac
user, and two runs started at the same time result in one run and one clean
"already running" exit.
**Effort:** 1 day.

### 5.4 Triggers (launchd)

Two LaunchAgents for the `ci` user in
`~/Library/LaunchAgents/`; templates are kept in `maestro/scripts/launchd/`:

| Agent | Schedule | What it does |
|---|---|---|
| `io.horizontalsystems.e2e.poll` | `StartInterval` 600 (every 10 min) | `poll-commits.sh`: fetches, compares the head SHA of the newest `version/*` branch with `~/e2e/last-tested-sha`, and if it changed runs `run-suite.sh --tags journey` and records the SHA. Several commits in 10 minutes become one run of the newest head |
| `io.horizontalsystems.e2e.nightly` | `StartCalendarInterval` 01:00 | `run-suite.sh --tags journey,feature,onchain` on the newest `version/*` branch |

- **Manual:** run `run-suite.sh` directly, with any branch and tags.
- **Repository access:** the clone is public and read-only, so fetching needs no
  GitHub credentials, and nothing on the Mac can write to GitHub.
- **Waking up:** if the Mac was off at 01:00, launchd runs the nightly job once
  at the next start-up.
- **Logs:** each agent's stdout and stderr go to `~/e2e/logs/`, rotated weekly
  by the maintenance job.
- **Later GitHub integration:** if the suite is ever wired back into GitHub
  Actions, `run-suite.sh` stays the entry point, and the workflow only calls it.

**Done when:**
- a new commit on the newest `version/*` branch produces a journey run within 10
  minutes, finishing in under 15 minutes end to end
- the nightly run starts at 01:00 on the newest `version/*` branch
- `launchctl list | grep e2e` shows both agents after a reboot
**Effort:** 0.5 day.

---

## Phase 6: Reporting

### 6.1 Results

- Each run gets its own directory, `~/e2e/runs/<timestamp>-<sha8>/`, with:
  - `report.xml` (`--format junit`), the machine-readable result
  - `report.html`, a readable summary generated from the JUnit file by the
    collect script (or by `--format html` if the pinned version supports
    writing both)
  - `output/` (`--test-output-dir`): a screenshot on failure,
    `commands-*.json` and `maestro.log`
  - `logcat-<serial>.txt`, captured per emulator for the whole run, so crashes
    have a stack trace
  - `meta.json`: branch, SHA, tags, start and end time, Maestro and system
    image versions
- `~/e2e/runs/latest` is a symlink to the newest run, and `~/e2e/index.html`
  lists the last 30 runs with their pass/fail counts. Open it in a browser on
  the Mac or over Screen Sharing.
- Run directories older than 30 days are deleted by the weekly maintenance job.

### 6.2 Recordings and redaction

- Each top-level flow records the screen through `onFlowStart`
  (`startRecording`) and `onFlowComplete` (`stopRecording`). After the run,
  `maestro/scripts/collect-artifacts.sh` deletes recordings of flows that
  passed.
- Results stay on the Mac, but they are what people copy into issues and chats,
  and the failure screenshot goes to Telegram. Because FLAG_SECURE is off in the
  e2e build, flows tagged `secret` must:
  - not record
  - keep no screenshots of restore pages
  - have the value removed from `commands-*.json` (the script replaces the
    phrase with `***`, if 1.3 question 6 shows Maestro logs it)
- The collect script marks the run as failed, and sends nothing to Telegram but
  a warning, if the raw secret string is found anywhere in the run directory
  after redaction.

### 6.3 Notifications

`maestro/scripts/notify-telegram.sh` posts to the existing bot and chat with
`curl` against the Bot API (`sendMessage`, and `sendPhoto` for screenshots).
The token and chat ID come from Keychain (4.2). It sends:

- **On failure (any trigger):** ❌, branch, short SHA, passed/failed/flaky
  counts, the names of failed flows, the path of the run directory on the Mac,
  and the failure screenshot of the first failed flow.
- **Nightly success:** a one-line ✅ with counts, as a daily heartbeat.
  Successful commit runs stay silent.
- **Infrastructure errors** (build failed, an emulator did not boot, the
  pre-flight backend check failed): ⚠️ with the step that failed, kept separate
  from test failures.
- **Silence alarm:** if no nightly message has been sent for 36 hours, the
  maintenance job sends a warning, so a dead Mac is noticed. If the Mac itself
  is down this cannot fire; a missing daily ✅ is then the signal.

### 6.4 Flakiness policy

- **Retry once:** after the main run, the script reruns only the failed feature
  and on-chain tests a single time. A failed journey step cannot be rerun on
  its own because it depends on the steps before it, so the whole journey is
  rerun once. A test that passes on retry counts as passing, but is reported as
  **flaky** in the summary and in Telegram.
- **History:** each run appends one JSON line per flow (flow, result, retry
  result, duration, branch, SHA) to `~/e2e-history/results.jsonl` on the runner.
  `maestro/scripts/flaky-report.py` computes the 14-day flake rate per flow and
  posts a weekly digest to Telegram.
- **Quarantine:** a flow that flakes 3 times, or more than 10% of runs in 14
  days, gets the `quarantine` tag and an issue labelled `e2e-flaky`. Quarantined
  flows still run nightly, but non-blocking and reported separately. A
  quarantined flow is fixed or deleted within 2 weeks.
- **Journey steps are never quarantined:** skipping a step would break the
  steps after it. A flaky journey step is fixed first, before other test work.
  If the fix takes longer, the step moves out of the journey into a feature
  test, which can then be quarantined.
- **Fixes:** a flaky flow is fixed by waiting on a concrete state. Adding sleeps
  or extra retries does not count as a fix.

**Files:** `maestro/scripts/collect-artifacts.sh`,
`maestro/scripts/notify-telegram.sh`, `maestro/scripts/flaky-report.py`, flow
headers.
**Done when:**
- a deliberately broken flow produces a Telegram message with its screenshot,
  and the run directory holds the JUnit and HTML reports, the screenshot, the
  recording and logcat
- a deliberately flaky flow is reported as flaky
- a `secret` flow's run directory contains no mnemonic
**Effort:** 2–3 days.

---

## Phase 7 (optional): LLM layer

Not part of the gate. Evaluate only after Phases 1–6 have been stable for a
month.

| Tool | Use | Notes |
|---|---|---|
| Maestro AI commands (`assertWithAI`, `assertNoDefectsWithAI`, `extractTextWithAI`) | Visual sanity checks inside existing flows | Check what the pinned version requires (API key, backend). Non-deterministic, so only in non-blocking flows |
| Arbigent | Scenario-driven exploratory runs with goals ("open swap, pick a pair, reach a quote") | Can share the emulator setup from Phase 5. Useful for finding dead ends and crashes |
| mobile-mcp / DroidRun | Drive the emulator from an agent to draft new Maestro flows from a plain-language scenario | The output is a draft. A human replaces text selectors with tags, adds assertions and tags |

Trade-offs and rules:

- **Determinism:** LLM runs never fail the build. They file findings, such as
  crashes from logcat and screenshots of dead ends, as issues for a human to
  triage.
- **Cost:** roughly one model call with a screenshot per step. Measure the cost
  on a 20-step session before scheduling anything, then cap the nightly
  exploratory budget per run.
- **Safety:**
  - LLM sessions only use public-vector or empty wallets, never the dust
    wallet. Screenshots leave the machine and FLAG_SECURE is off.
  - The broadcast guard (2.2) stays on.
  - Agents run on a separate emulator instance, never the one running the
    regression suite.

**Done when:** a written comparison of 2 tools on 3 scenarios (cost per run,
useful findings, false alarms), and a decision to adopt one or drop the phase.
**Effort:** 3–5 days.

---

## Risks and mitigations

| Risk | Mitigation |
|---|---|
| Live mainnet RPCs and the dev backends (`api-dev.blocksdecoded.com`, `swap-dev.unstoppable.money`) are slow or down, so on-chain flows fail | On-chain flows run nightly only, never on push. Each waits on a concrete state with a generous timeout. A pre-flight flow checks that the backends are reachable, so a failure is reported as infrastructure, not app |
| UI changes break selectors | `testTag`s from 2.3, the convention in `AGENTS.md`, and the journey run on every new `version/*` commit catch breakage within one merge |
| An early journey failure hides problems in later steps | The failing step is named in the report and Telegram, so it gets fixed first. Cases the journey cannot afford to lose coverage on also have feature tests, which run independently every night |
| Developers skip writing feature tests | The `AGENTS.md` rule and the pull request checklist line (2.4), the template (3.3), and a ready-wallet setup in one call keep the cost of a test low |
| Emulator flakiness (boot hangs, ANRs, stale state) | Read-only instances from one snapshot, booted fresh every run. Pinned system image and Maestro version. Boot health check in `device-prepare.sh`. Weekly maintenance |
| Keystore auth window expires on a long-running emulator (C2) | Emulators are booted per run, and `device-prepare.sh` unlocks the keyguard each time |
| A flow taps Send on a real wallet | No flow taps `confirm_send`, the broadcast guard (2.2) blocks it in e2e builds, and the dust wallet holds only a few dollars |
| A secret leaks through screenshots, logs or Telegram | Secrets only in Keychain, the `secret` tag disables recording, the collect script redacts and then blocks the Telegram upload on any remaining match, and the wallet is treated as burnable |
| Public test addresses receive dust or spam, so history assertions break | Assert specific known transactions, never counts |
| Results are visible only on the Mac, so failures go unread | Telegram carries the failed flow names and screenshot. `~/e2e/index.html` lists recent runs. The silence alarm reports a stopped nightly |
| The polling job misses commits, or the Mac is down when commits land | Each poll tests the newest head, so missed intermediate commits are covered by the next run. After downtime, the first poll catches up and launchd runs the missed nightly once |
| F-Droid flavor drifts from the Play flavor (WalletConnect and passkeys exist only in `base`/`ci`, C10) | Accept for now. When WalletConnect coverage is wanted, add a `ciE2e` variant and a separate tag |
| Heavy chains (Zcash, Monero, Zano) regress unnoticed | Out of scope by decision. Native ARM emulators keep the option open; add them as a weekly job later |
| Coverage below API 35 is missing (`minSdk` is 28) | Add a weekly run on an API 30 arm64 image once the suite is stable. Older versions are covered by manual QA on physical devices |
| Mac hardware fails or a macOS update breaks the emulator | Setup is scripted (5.1), so a replacement machine can be brought up in an hour. macOS updates are applied manually, followed by a manual journey run |

---

## First week checklist

**Day 1: proof of concept**
- [ ] Install the pinned Maestro version and the `android-35;google_apis;arm64-v8a` image on your Mac
- [ ] Create AVD `e2e_api35` and run `adb shell locksettings set-pin 1111`; set the three animation scales to 0
- [ ] `./gradlew :app:assembleFdroidCiDebug`, install it, and run `intro.yaml` and `create_wallet_standard.yaml` with `APP_ID=io.horizontalsystems.bankwallet.fdroidci.dev`
- [ ] Fix any F-Droid differences in the sub-flows

**Day 2: open questions**
- [ ] Answer the six questions in 1.3 and write them into `maestro/README.md`
- [ ] Run the two flows 5× headless (`-no-window -read-only`) to confirm stability

**Day 3: build type and hooks**
- [ ] Add the `e2e` build type and the variant filter (2.1); check that release output is unchanged
- [ ] Add `e2eBuild` to `IAppConfigProvider` and the rooted, FLAG_SECURE, rate-app and donation hooks (2.2)
- [ ] Locate the broadcast choke point and add the guard

**Day 4: first selectors**
- [ ] Enable `testTagsAsResourceId` at the root and in `BottomSheet.kt` / `HSBottomSheet.kt`
- [ ] Add `TestTags.kt` and tag intro, tabs, terms, create, restore and balance (the screens the first journey steps touch)
- [ ] Rewrite `accept_terms.yaml` and `create_standard_wallet.yaml` to use `id:` selectors

**Day 5: structure and repeatability**
- [ ] Move the existing flows into `journey/` and `onchain/` as mapped in 3.1, and check that `executionOrder` runs the journey steps in order
- [ ] Write `subflows/setup/wallet_ready.yaml` and `features/_template.yaml`
- [ ] Replace the "abandon" wallet in the send flow with a placeholder for `MAESTRO_E2E_DUST_MNEMONIC`, and create the dust wallet (not yet funded)
- [ ] Write `emulators-up.sh` and `device-prepare.sh`, and run the journey and a feature test on 2 parallel emulators
- [ ] Order the runner Mac (if it isn't there yet), create the `ci` macOS user, and store the dust mnemonic and the Telegram token in its Keychain

## Effort summary

| Phase | Effort |
|---|---|
| 1. Local proof of concept | 2 days |
| 2. App-side preparation | 5–6 days |
| 3. Core flow suite | 6.5–8.5 days |
| 4. Secrets and test data | 1 day |
| 5. Always-on runner | 3.5 days |
| 6. Reporting | 2–3 days |
| 7. LLM layer (optional) | 3–5 days |
| **Total without Phase 7** | **about 20–24 days (4–5 weeks for one engineer)** |
