# AGENTS.md

## Project Overview

Unstoppable Wallet - A multi-currency crypto wallet Android app supporting Bitcoin, Ethereum, and many other blockchains.

- **Package**: `io.horizontalsystems.bankwallet`
- **Min SDK**: See `build.gradle` for `min_sdk_version`
- **Language**: Kotlin
- **UI**: Jetpack Compose (migrating from XML, see `ui/` vs `uiv3/`)

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Lint
./gradlew lint
```

## Project Structure

```
app/src/main/java/io/horizontalsystems/bankwallet/
├── core/                    # Core infrastructure
│   ├── managers/           # Business logic managers (singletons via App)
│   ├── adapters/           # Blockchain adapters
│   ├── factories/          # Factory classes
│   └── providers/          # Data providers
├── entities/               # Data models
├── modules/                # Feature modules (MVVM)
│   ├── balance/           # Wallet balance
│   ├── send/              # Send transactions
│   ├── multiswap/         # Token swaps
│   └── ...
├── ui/                     # Legacy Compose components
└── uiv3/                   # New Compose components (prefer this)

subscriptions-core/         # Subscription/paid features (IPaidAction)
subscriptions-google-play/  # Google Play billing
subscriptions-fdroid/       # F-Droid flavor
```

## Architecture Patterns

### ViewModel Pattern
```kotlin
class MyViewModel(...) : ViewModelUiState<MyUiState>() {
    override fun createState() = MyUiState(...)

    // Call emitState() after state changes
}
```

### Service Pattern (for business logic)
```kotlin
class MyService(...) : ServiceState<MyServiceState>() {
    override fun createState() = MyServiceState(...)

    // Call emitState() after state changes
}
```

### Clearable Interface
Implement `Clearable` for classes that need cleanup:
```kotlin
class MyService : Clearable {
    override fun clear() {
        coroutineScope.cancel()
    }
}
```

## Dependency Injection

Uses manual DI via `App` companion object:
```kotlin
// Declaration
companion object {
    lateinit var myManager: MyManager
}

// Initialization in App.onCreate()
myManager = MyManager(dependency1, dependency2)

// Usage
App.myManager.doSomething()
```

## Reactive Patterns

- **Preferred**: Kotlin Coroutines + `StateFlow` / `SharedFlow`
- **Deprecated**: RxJava (legacy code only, do not use in new code)

```kotlin
// Preferred pattern - use StateFlow
class MyRepository {
    private val _itemsFlow = MutableStateFlow<List<Item>>(emptyList())
    val itemsFlow: StateFlow<List<Item>> = _itemsFlow.asStateFlow()
}

// In ViewModel
viewModelScope.launch {
    repository.itemsFlow.collect { items ->
        // handle
    }
}
```

### Migration Note
Existing RxJava code should be migrated to Coroutines/Flow when modified. Replace:
- `Observable` → `Flow` / `StateFlow`
- `BehaviorSubject` → `MutableStateFlow`
- `PublishSubject` → `MutableStateFlow`
- `Single` → `suspend fun`
- `Completable` → `suspend fun` returning `Unit`

## Storage

### SharedPreferences via LocalStorageManager
```kotlin
// Add to ILocalStorage interface
var myPreference: Boolean

// Implement in LocalStorageManager
override var myPreference: Boolean
    get() = preferences.getBoolean(KEY, defaultValue)
    set(value) = preferences.edit().putBoolean(KEY, value).apply()
```

### Room Database
- Database: `AppDatabase`
- DAOs in `appDatabase`

## Blockchain Support

Each blockchain has:
- **Adapter**: `ISendXxxAdapter` in `core/adapters/`
- **Kit**: Separate module (e.g., `ethereumkit`, `bitcoinkit`)
- **SendTransactionService**: `modules/multiswap/sendtransaction/`

Supported chains: EVM (Ethereum, BSC, etc.), Bitcoin, Solana, Tron, TON, Zcash, Monero, Stellar

### Adding or changing a chain

Follow `docs/chain-integration/README.md`: delta matrix before code, history
test vectors from live addresses, role-separated review, and the hand-off to
iOS. Check the design and the review against
`docs/chain-integration/checklist.md` and cite item IDs (`AMT-2`, `TX-4`) in
the delta matrix and findings. With Claude Code, run `/chain-review <chain>`
for step 4.

## Subscription/Paid Features

```kotlin
// Check if action is allowed
UserSubscriptionManager.isActionAllowed(AdvancedSearch)

// Paid actions defined in IPaidAction
object AdvancedSearch : IPaidAction
object TradeSignals : IPaidAction
// etc.
```

The Defense System actions (`SecureSend`, `ScamProtection`, `SwapProtection`)
are free: they are not part of the Premium subscription, so never gate them
with `isActionAllowed`. `ScamProtection` and `SwapProtection` follow the user's
switch in `PaidActionSettingsManager`; Swap Protection also needs a route that
supports MEV protection. Secure Send has no switch of its own: each address
check (`AddressCheckType`) is enabled separately.

## Conventions

- **Naming**: PascalCase for classes, camelCase for functions/variables
- **State classes**: Suffix with `State` or `UiState`
- **ViewModels**: Suffix with `ViewModel`
- **Services**: Suffix with `Service`
- **Repositories**: Suffix with `Repository`
- **Compose**: New components go in `uiv3/components/`
- **Design kit**: The `designkit` module (`io.horizontalsystems.designkit`)
  holds what the Figma design system file defines: colours and typography
  in `theme`, and in `components` the shared components confirmed against
  Figma (the button controls so far). Use those instead of adding a button
  elsewhere. It depends on no app code, so nothing from `walletkit` or
  `app` may be imported there.

### Preventing Double Clicks in Compose

Use **local composable state** (not ViewModel) to disable buttons during async operations. This prevents double-clicks because local state updates are synchronous, while ViewModel state requires an async round-trip.

```kotlin
// Correct: Local state — immediate, no race condition
var buttonEnabled by remember { mutableStateOf(true) }

ButtonPrimaryYellow(
    enabled = buttonEnabled,
    onClick = {
        buttonEnabled = false  // Immediate, same frame
        coroutineScope.launch {
            try {
                viewModel.doAction()
            } finally {
                buttonEnabled = true
            }
        }
    }
)

// Wrong: ViewModel state — race condition possible
// User can click twice before recomposition disables button
ButtonPrimaryYellow(
    enabled = uiState.buttonEnabled,  // Async update
    onClick = { viewModel.doAction() }
)
```

## Strings and Translations

User-facing text lives in `walletkit/src/main/res/values/strings.xml`. The app
ships the languages listed in the `LocaleType` enum
(`walletkit/src/main/java/io/horizontalsystems/walletkit/helpers/LocaleHelper.kt`):
English as the source plus de, es, pt-BR, fa, fr, ko, ru, tr, zh.

Translations are part of the change that touches the strings. There is no
separate translation step, and CI fails a pull request when a key is missing
from any locale or its placeholders differ from English
(`.github/scripts/check_translations.py`). When you add, change, or remove a
string:

- Add the translated entry to every locale file in the same change:
  `values-de`, `values-es`, `values-pt-rBR`, `values-fa`, `values-fr`,
  `values-ko`, `values-ru`, `values-tr`, `values-zh`. Append new entries at
  the end of each file, before `</resources>`.
- When the English text changes, update all nine translations. When a key is
  removed or renamed, remove or rename it in all nine files.
- Skip entries marked `translatable="false"`; they exist only in the English
  file.
- Keep format placeholders exactly as in English (`%s`, `%d`, `%1$s`) and use
  Android escaping (`\'`, `\"`, `\n`), never XML entities such as `&apos;`.
- Use established crypto terminology (wallet, token, swap, gas fee, private
  key, recovery phrase). Leave proper nouns, blockchain names, and ticker
  symbols untranslated.

## Common Gotchas

1. **RxJava to Flow (legacy)**: If Observable completes, `.asFlow()` stops collecting silently — migrate to pure Flow
2. **Coroutine exceptions**: Uncaught exceptions in `launch` blocks terminate silently — wrap in try-catch or use `catch` operator
3. **Mutex in Services**: Use `mutex.withLock` for thread-safe state updates
4. **ViewModel cleanup**: Call `service.clear()` in `onCleared()`
5. **Flow collection**: Always collect in a coroutine scope that matches the lifecycle (e.g., `viewModelScope`)

## Testing

- Unit tests in `src/test/` of the module that holds the code (`walletkit`,
  `walletkit-chain-<chain>`, `app`)
- Instrumentation tests in `app/src/androidTest/`
- Maestro UI tests, run against the `ciE2e` build by the test station
  (`horizontalsystems/uw-testlab`). The flows are shared with iOS and live
  in appkb (`flows/` on its `flows` branch). How to fetch and run them:
  `maestro/README.md`. How to write a flow: the flows' own `README.md`

### Tests a change needs

Every change that alters behaviour adds or updates its tests in the same
change, written by whoever makes the change. Decide which tests apply before
calling the work done:

| Change | Test |
|---|---|
| User-facing feature added or changed: a new screen, flow, or behaviour the user sees | Test tags on its controls and a description of the behaviour, see [UI tests for features](#ui-tests-for-features) |
| Logic: amounts, fees, parsing, address validation, sync or state handling in a ViewModel, Service, or adapter | Unit tests for the changed behaviour and its edge cases |
| Bug fix | A unit test that fails without the fix. When only the UI shows the bug, the steps that reproduce it in the pull request description instead |
| A chain added or changed | As `docs/chain-integration/README.md` says |
| A test tag renamed or removed | The flows in appkb that select it |

No new test is needed for a refactor that keeps behaviour, for strings and
translations, styling, build configuration, or dependency updates. Existing
tests must still pass.

Run the tests you added or touched before finishing. When a needed test cannot
be written, for example because it needs a hardware wallet, a paid
subscription, or a real broadcast, say which test is missing and why in the
pull request description and in your summary to the developer.

### UI test tags

Maestro selects elements by test tag (`id:` in a flow). Tags are added while
the code is written, in the same change as the control, never later.

**When.** Give a test tag to every control the user interacts with, and to
every value a test checks. A control is anything given an `onClick*`,
`onValueChange` or `onCheckedChange` lambda, or a `Modifier.clickable`,
`toggleable` or `selectable`. Do not wait for a test to need it. A screen
or container that only passes callbacks down has no tag of its own; the
controls inside it do.

**Name protocol.**

- Names are declared only in code: `TestTags` in
  `walletkit/.../uiv3/TestTags.kt`. appkb holds the shared flows, not a list
  of names; the flows select names as plain strings (`id: send_next`).
- Form: `<screen>_<element>` in snake_case (`send_next`, `confirm_amount`).
  `<screen>` is the screen's name without `Screen`/`Page`.
- A name with a variable part is a function. Add a suffix (coin code,
  blockchain uid, index, word) only where a test has to pick one item of
  a list: `fun balanceItem(coinCode: String) = "balance_item_$coinCode"`.
- No app is the source of truth. A name belongs to whichever app declares
  it first, and both apps use the same string. Before declaring a name,
  look for the control in, in this order:
  1. the shared flows (appkb `flows/`): a name a flow selects is the
     contract, and an app that differs renames;
  2. both apps' `TestTags`: this app's, and the iOS app's
     (`packages/WalletCore/Sources/WalletCore/UserInterface/TestTags.swift` in
     `horizontalsystems/unstoppable-wallet-ios`, readable with
     `gh api repos/horizontalsystems/unstoppable-wallet-ios/contents/<path>`;
     add `?ref=<branch>` when it is not on the default branch yet).

  Reuse a name you find. Only when none exists, declare a new one.
- One control, one name. If the two apps already named a control
  differently and no flow selects either name, the older name wins.
- Apply it with `Modifier.testTag(TestTags.X)` through the component's
  `modifier` parameter. If a shared component has none, add one rather than
  wrapping it.
- Tags reach Maestro only inside `Modifier.exposeTestTags()`. The activity
  root has it; a new `Dialog` or `ModalBottomSheet` needs it on its
  content, because it opens its own window.
- A name is renamed or removed only together with every flow in appkb that
  selects it.

**Undeclared names fail silently.** A flow step that selects an undeclared
name fails the run, unless the step is conditional (`when: visible`,
`optional: true`). Then Maestro skips it and the flow passes. When you add
an `id:` to a flow, check that `TestTags` declares it. CI fails a pull
request when a flow on appkb's `flows` branch selects a name `TestTags` does
not declare, outside steps that run only on iOS
(`.github/scripts/check_test_tags.py`). So a pull request that renames or
removes a tag a flow selects fails until appkb is changed too.

### UI tests for features

A pull request does not write Maestro tests; it adds test tags only. As soon
as a pull request is merged, a job on the test station reads its code and
description, writes Maestro tests for its new features into appkb's `flows`
branch, selecting their test tags, and runs them. It can only test what the pull request gives it:

- a test tag on every control and every checked value, as
  [UI test tags](#ui-test-tags) says;
- a pull request description that says what the user does and what they see
  as a result. The test checks that, so a bug in the code does not become
  the expected behaviour.

## Git Commit Messages

Use **plain imperative mood**, no type prefixes (`feat:`, `chore:`, etc.):

```
Update provider data          ✓
Fix apostrophe in strings.xml ✓
chore: update provider data   ✗
```

The message should complete: *"If I add this commit, it will [message]."*  
Summary line is **60 characters max**. Single-line for small changes; add a blank line + body for context when the why is non-obvious.

## Issues, Pull Requests, and Commits

Issues, pull requests, and commits each answer a different question. Put
information where it answers its question, and prefer the most durable place:
commit messages and code comments travel with the code, while issues and pull
requests stay on GitHub and are rarely read once closed.

All three contain only their own content. No footers, signatures, session
links, or notes about the editor or tool the text or change was made with.
Git already records the author and date, and the review history lives on
GitHub, so such lines add nothing a reader can act on and go stale as tools
change.

### Issue: what do we need, and why?

An issue is read long after it is closed, by people and by agents, and
usually without its pull requests. It says only what is needed and why,
in a few lines; the pull requests say how it was done and verified, and
their commits say what each change does. Nothing that lives in one of
those belongs in the issue.

An issue is either a bug or a task, and the two are written differently.
Decide which from the request itself: a bug names behaviour that is wrong
today against what was already expected (a crash, a wrong value, something
that used to work); a task names something that should exist or change (a
design to match, a feature to add, a flow to rework). A request that mixes
both is a task, and the bugs found on the way are part of what it fixes.
Ask the developer only when the request reads both ways.

A bug:

- What is wrong, and what was expected instead.
- How to reproduce it: app version, device, chain, steps.

A task:

- What should be different when the work is done, and why, in a sentence
  or two.
- Where the source of truth is, in a form a reader can open: the server
  API document, a Figma link, a product decision. Say what to ignore in
  it. Another platform's implementation is not a source of truth; when
  one exists, it may be linked as something to compare against.
- A decision the source of truth leaves open, one line each, only when
  the work would otherwise stop to ask.

Both describe the outcome, not the implementation, and leave out what the
repository already says: conventions, architecture, file names, and
implementation steps. A solution plan written into an issue goes stale as
soon as work starts. An issue is closed when the outcome is reached, which
may take several pull requests.

### Pull request: is this change right and safe to merge?

- Which issue it addresses (link it).
- Why this approach, and which alternatives were ruled out.
- How to verify it.
- What is risky and deserves a close look.
- For a user-facing change, what the user does and sees, which its UI test
  is written from ([UI tests for features](#ui-tests-for-features)).
- Dependencies and merge order, or the target branch for a backport.

Cover only the points above that apply, each in a sentence. Most pull
requests need one or two. Do not list the changes; the diff and the commit
messages already say that.

Closing keywords (`Closes #123`) only take effect when a pull request merges
into the default branch (`master`). Pull requests target `develop`, so close
issues by hand.

### Commit: what does this change do, and why?

A commit message must be understandable without the issue or the pull
request. See [Git Commit Messages](#git-commit-messages) for the format.

### Code comments: why is this code the way it is?

Explain what the code cannot show by itself: an invariant, an ordering that
matters, a reason something cannot be simplified. Do not describe what the
code does or how it got here. When the same explanation applies in several
places, it belongs in `docs/` or in this file instead.
