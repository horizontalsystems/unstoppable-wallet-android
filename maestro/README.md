# Maestro UI Tests

Run a single test:

```bash
maestro test -e APP_ID=io.horizontalsystems.bankwallet.appcenter.dev maestro/onboarding/intro.yaml
```

Run all tests:

```bash
maestro test -e APP_ID=io.horizontalsystems.bankwallet.appcenter.dev maestro/
```

## Test structure

The suite is moving to the layout below; see
[`docs/testing/maestro-integration-plan.md`](../docs/testing/maestro-integration-plan.md)
for the full plan. The flows in `onboarding/` and `wallet/` predate it and move
into this layout as the plan is carried out.

There are two kinds of tests:

- **The journey**: one test made of ordered steps that walk the main user path
  on a single app install, the way a real user would.
- **Feature tests**: small independent tests, one or more per feature, added by
  the developer who builds the feature.

```
maestro/
├── journey/            # the main user path, ordered steps
├── features/           # one folder per feature, independent tests
│   ├── _template.yaml
│   └── <feature>/<scenario>.yaml
└── subflows/           # reusable steps: onboarding, wallet setup, PIN, navigation
    └── setup/wallet_ready.yaml
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
├── 04_set_pin.yaml          # set PIN, lock, unlock, wrong PIN rejected
├── 05_enable_token.yaml     # Manage coins → search USDT → enable → shows on balance
├── 06_receive.yaml          # receive screen shows an address
├── 07_delete_wallet.yaml    # delete the created wallet
├── 08_restore_wallet.yaml   # restore from the public test phrase
├── 09_check_addresses.yaml  # exact BTC/ETH addresses match the test vector
├── 10_send_form.yaml        # send form: address, amount, "insufficient balance"
├── 11_history.yaml          # transactions screen opens
├── 12_swap.yaml             # swap tab, pick a pair, quote or error appears
└── 13_settings.yaml         # App Status, currency, theme
```

`journey/config.yaml`:

```yaml
flows:
  - "*.yaml"
executionOrder:
  continueOnFailure: false   # a later step makes no sense once an earlier one failed
  flowsOrder:
    - 01_onboarding
    - 02_create_wallet
    # ... through 13_settings
```

The first step clears the app:

```yaml
appId: ${APP_ID}
name: "Journey 01 · Onboarding"
tags: [journey]
---
- launchApp:
    clearState: true
- runFlow: ../subflows/onboarding/pass_intro.yaml
- assertVisible:
    id: onboarding_new_wallet
```

A later step continues from the current state:

```yaml
appId: ${APP_ID}
name: "Journey 05 · Enable a token"
tags: [journey]
---
- tapOn:
    id: tab_balance
- tapOn:
    id: balance_manage_coins
- tapOn:
    id: search_input
- inputText: "USDT"
- tapOn:
    id: manage_coin_toggle_tether_ethereum
- back
- assertVisible:
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
- If `executionOrder` turns out not to work with a separate config for this
  folder, the fallback is a single `journey.yaml` that calls the steps in
  order with `runFlow`. The failing step then shows by name in the log rather
  than as a separate report entry.

### Feature tests

Each feature test is independent and starts from a ready wallet in one line,
so it only contains the steps for its feature:

```yaml
appId: ${APP_ID}
name: "Contacts · add a contact from the send screen"
tags: [feature, contacts]
---
# Ready state: app cleared, onboarding passed, public test wallet restored with BTC
- runFlow:
    file: ../../subflows/setup/wallet_ready.yaml
    env:
      CHAINS: "bitcoin"

- tapOn:
    id: tab_settings
- tapOn:
    id: settings_contacts
- tapOn:
    id: contacts_add
# ... the feature's own steps and assertions
```

Rules for feature tests:

- A pull request that adds or changes a user-facing feature adds or updates a
  test under `features/<feature>/`. Start from `features/_template.yaml`.
- Always start with `subflows/setup/wallet_ready.yaml`. Never depend on another
  test or on the order tests run in.
- Keep each test under 2 minutes.
- Select elements by test tag (`id:`). New screens get their test tags in the
  same pull request.
- Run it locally before opening the pull request:

  ```bash
  maestro test -e APP_ID=io.horizontalsystems.bankwallet.fdroidci.e2e maestro/features/<feature>
  ```

- When a feature becomes part of the main user path, add a step for it to the
  journey and delete the feature test.

### When tests run

| Tests | Tag | When |
|---|---|---|
| Journey (about 8 min) | `journey` | Every new commit on `version/*`, and nightly |
| Feature tests | `feature` | Nightly, and locally by the developer while working on the feature |
| Real send to confirm (funded wallet), history with known transactions | `onchain` | Nightly |
