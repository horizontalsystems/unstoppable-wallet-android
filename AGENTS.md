# AGENTS.md

## Project Overview

Unstoppable Wallet - A multi-currency crypto wallet Android app supporting Bitcoin, Ethereum, and many other blockchains.

- **Package**: `io.horizontalsystems.bankwallet`
- **Min SDK**: `minSdk` in `gradle/libs.versions.toml`
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

# Code smells (Detekt)
./gradlew detekt

# Architecture rules (Konsist)
./gradlew :architecture-tests:test
```

## Static Analysis

Both checks must pass before a pull request is opened, and again after every
later push to it. Run them yourself: no CI job does it yet. When one fails,
fix the code; do not open the pull request with a failure and a note about
it. Neither check compiles the app, so they take seconds.

    ./gradlew detekt :architecture-tests:test

The steps still to come (a CI check, a weekly review of the whole project)
are in `docs/code-quality-plan.md`.

**Detekt** reports code smells: complexity, long methods, swallowed
exceptions, and Compose mistakes through the Compose Rules plugin. The
overrides are in `config/detekt/detekt.yml`. Findings that predate Detekt are
recorded in `config/detekt/baseline.xml`, so only new ones fail the run.

- Fix a new finding; do not add it to the baseline.
- A baseline entry names the rule, the file and the declaration, not the
  line. A second finding of the same rule in the same declaration, such as
  another swallowed exception in an already listed function, is therefore
  not reported. When you change a declaration the baseline lists, check it
  for that rule yourself.
- When a change removes baselined findings, shrink the baseline with
  `./gradlew detektBaseline` and commit the result. Check the diff only
  removes entries.
- A rule that is wrong for this project is switched off in `detekt.yml` with
  a comment that says why, not suppressed case by case.

**Konsist** holds the rules of this file that can be read off the source, as
unit tests in `architecture-tests`: module layering, no RxJava, naming, test
tags. It has no baseline: a rule must hold for the whole project, with a known
exception named in the rule itself. When a rule in this file can be checked
that way, add its test in the same change.

## Project Structure

The wallet's code lives in `walletkit`; `app` is a thin shell around it.
Dependencies point one way: `app` depends on `walletkit` and on every
`walletkit-chain-<chain>`; each chain module depends on `walletkit`;
`walletkit` depends on `designkit`, `components/icons`, `components/chartview`,
`subscriptions-core` and `dapp-core`. `walletkit` and the chain modules import
nothing from `app`, and `designkit` imports nothing from the other modules of
the project; `architecture-tests` checks both. `app` plugs in, per flavor, one
`subscriptions-*` implementation and, where the flavor has it,
`dapp-wallet-connect`; the wiring is in `app/build.gradle.kts`.

```
app/                         # Thin shell: MainApp, MainActivity, build config provider, flavors, build types
walletkit/src/main/java/io/horizontalsystems/walletkit/
├── core/                    # Core infrastructure
│   ├── managers/           # Business logic managers (singletons via App)
│   ├── adapters/           # Blockchain adapters
│   ├── factories/          # Factory classes
│   ├── providers/          # Data providers
│   └── chain/              # Chain plugin interfaces
├── entities/               # Data models
├── modules/                # Feature modules (MVVM)
│   ├── balance/           # Wallet balance
│   ├── send/              # Send transactions
│   ├── multiswap/         # Token swaps
│   └── ...
├── ui/                     # Legacy Compose components
└── uiv3/                   # New Compose components (prefer this), TestTags

walletkit-chain-<chain>/     # One module per blockchain, same package root and folders as walletkit
designkit/                   # Figma design system: theme and generic components
components/icons/            # Drawables and coin icons
components/chartview/        # Chart view
subscriptions-core/          # Subscription/paid features (IPaidAction)
subscriptions-dev/           # Subscription service for dev and test builds
subscriptions-google-play/   # Subscription service on Google Play billing; in the build only if its directory exists
subscriptions-fdroid/        # Subscription service for the F-Droid flavors
dapp-core/                   # dApp connection abstraction
dapp-wallet-connect/         # WalletConnect implementation of it; in the build only if its directory exists
architecture-tests/          # Konsist rules, a JVM module outside the app
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

Each blockchain is one `walletkit-chain-<chain>` module, in the same package
root and folders as `walletkit`:
- **Adapter**: the chain's adapters in `core/adapters/`, with an
  `ISend<Chain>Adapter` interface for sending
- **Kit**: the blockchain kit library the module depends on (`libs.kit.*` in
  `gradle/libs.versions.toml`; Zcash uses the Zcash Android SDK)
- **SendTransactionService**: `modules/multiswap/sendtransaction/` of the chain
  module
- **ChainPlugin**: `chain/<chain>/` of the chain module, registered in
  `MainApp.registerChainPlugins()`; the interface is `ChainPlugin` in
  `walletkit`'s `core/chain/`

Each supported chain family is one `walletkit-chain-<chain>` module, so the
supported chains are those modules. Bitcoin's forks live in the bitcoin module,
and Maya in the thorchain module.

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
- **Compose**: Where a new component goes depends on its layer, see
  [UI work](#ui-work)
- **Design kit**: The `designkit` module (`io.horizontalsystems.designkit`)
  holds what the Figma design system file defines: colours and typography
  in `theme`, the icons in `res/drawable`, and in `components` the shared
  components confirmed against Figma (the button controls and the text
  input so far). Use those instead of adding a button or a text input
  elsewhere. It depends on no app code, so nothing from `walletkit` or `app`
  may be imported there.

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

## UI work

### Layers

- `designkit` is the bottom layer: tokens and generic components defined in
  the Figma design system file. It imports nothing from `walletkit` or `app`.
- Wallet-specific components (a coin icon, a token amount input) take plain
  display data and do not depend on `walletkit`. Their module is created when
  the first one is ready; until then they stay in `uiv3/components/`.
- Screens live in `walletkit` and build on both.
- New generic components go into `designkit`, never into `walletkit`'s `ui/`
  or `uiv3/`. Screens migrate one pull request each with their logic
  untouched; an old component is deleted with its last usage.
- Icons live in `designkit`, and only icons confirmed in Figma are added
  there. The rules, and the icons that stay outside, are in
  `docs/ui-refactoring.md`.

### Moving a component into `designkit`

- Name the component, its properties and its values as Figma does, so they
  can be searched there. No renames for code style.
- Its icons follow `docs/ui-refactoring.md`: one that Figma lacks stays
  outside and is passed in by the caller.
- Use theme text styles (`ComposeAppTheme.typography.*`), not the
  `body_leah`-style helpers from `walletkit`'s `Text.kt`.

### Reading a design

- The linked Figma frame may be the iOS one. Use its Android twin on the same
  page and report where the two differ.
- List the page's other frames and ask whether they are in scope before
  calling the issue done.
- Compare the frame with the current screen first and report the differences.
  Where the design is silent, ask.
- Keep Figma's values even when they look wrong, such as low contrast. Raise
  the question; do not deviate silently.

### Verifying

- Confirm the component you changed is the one the screens use.
- Check a new component inside horizontally scrolling rows, not only in
  bounded layouts.
- Check light and dark on a device and compare before and after side by side.
- Use a test device with the end-to-end build. Never clear or overwrite an
  app that holds real wallets.

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
- UI tests select controls by test tag, see [UI test tags](#ui-test-tags)

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

No new test is needed for a refactor that keeps behaviour, for strings and
translations, styling, build configuration, or dependency updates. Existing
tests must still pass.

Run the tests you added or touched before finishing. When a needed test cannot
be written, for example because it needs a hardware wallet, a paid
subscription, or a real broadcast, say which test is missing and why in the
pull request description and in your summary to the developer.

### UI test tags

UI tests select elements by test tag. Tags are added while the code is
written, in the same change as the control, never later.

**When.** Give a test tag to every control the user interacts with, and to
every value a test checks. A control is anything given an `onClick*`,
`onValueChange` or `onCheckedChange` lambda, or a `Modifier.clickable`,
`toggleable` or `selectable`. Do not wait for a test to need it. A screen
or container that only passes callbacks down has no tag of its own; the
controls inside it do.

**Name protocol.**

- Names are declared only in code: `TestTags` in
  `walletkit/.../uiv3/TestTags.kt`.
- Form: `<screen>_<element>` in snake_case (`send_next`, `confirm_amount`).
  `<screen>` is the screen's name without `Screen`/`Page`.
- A name with a variable part is a function. Add a suffix (coin code,
  blockchain uid, index, word) only where a test has to pick one item of
  a list: `fun balanceItem(coinCode: String) = "balance_item_$coinCode"`.
- No app is the source of truth. A name belongs to whichever app declares
  it first, and both apps use the same string. Before declaring a name,
  look for the control in both apps' `TestTags`: this app's, and the iOS
  app's (`packages/WalletCore/Sources/WalletCore/UserInterface/TestTags.swift`
  in `horizontalsystems/unstoppable-wallet-ios`). Read the iOS file from
  GitHub with
  `gh api -H 'Accept: application/vnd.github.raw+json' 'repos/horizontalsystems/unstoppable-wallet-ios/contents/<path>'`,
  which prints the file itself, never from a local checkout, which may be
  stale. Read the default branch first, always; when the counterpart work
  sits on a branch not merged yet, read that branch too, with
  `?ref=<branch>` inside the quotes. Reuse a name you find. Only
  when none exists, declare a new one.
- One control, one name. Where the two apps already named a control
  differently, the older name wins.
- A declared name is kept: a control that replaces another takes its name.
- A control only this app has takes a name by the same convention; nothing
  more is asked of the developer.
- Apply it with `Modifier.testTag(TestTags.X)` through the component's
  `modifier` parameter. If a shared component has none, add one rather than
  wrapping it.
- Tags reach UI tests only inside `Modifier.exposeTestTags()`. The activity
  root has it; a new `Dialog` or `ModalBottomSheet` needs it on its
  content, because it opens its own window.

### UI tests for features

A pull request adds test tags and a description of the feature; it writes
no UI tests. A test for a new feature can only check what the pull request
gives it:

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

Before opening one, make sure Detekt and the architecture tests pass, see
[Static Analysis](#static-analysis).

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
