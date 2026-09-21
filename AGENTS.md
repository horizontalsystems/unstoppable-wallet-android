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

## Subscription/Paid Features

```kotlin
// Check if action is allowed
UserSubscriptionManager.isActionAllowed(SwapProtection)

// Paid actions defined in IPaidAction
object SwapProtection : IPaidAction
object SecureSend : IPaidAction
// etc.
```

## Conventions

- **Naming**: PascalCase for classes, camelCase for functions/variables
- **State classes**: Suffix with `State` or `UiState`
- **ViewModels**: Suffix with `ViewModel`
- **Services**: Suffix with `Service`
- **Repositories**: Suffix with `Repository`
- **Compose**: New components go in `uiv3/components/`

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

- Unit tests in `app/src/test/`
- Instrumentation tests in `app/src/androidTest/`

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

- What is wrong or missing.
- How to reproduce it: app version, device, chain, steps.
- Why it matters: who is affected and how badly.
- What done looks like, and what is out of scope.

Describe the problem, not the implementation. A solution plan written into an
issue goes stale as soon as work starts. An issue is closed when the problem
is solved, which may take several pull requests.

### Pull request: is this change right and safe to merge?

- Which issue it addresses (link it).
- Why this approach, and which alternatives were ruled out.
- How to verify it.
- What is risky and deserves a close look.
- Dependencies and merge order, or the target branch for a backport.

Cover only the points above that apply, each in a sentence. Most pull
requests need one or two. Do not list the changes; the diff and the commit
messages already say that.

Closing keywords (`Closes #123`) only take effect when a pull request merges
into the default branch (`master`). Pull requests target `version/*`, so close
issues by hand.

### Commit: what does this change do, and why?

A commit message must be understandable without the issue or the pull
request. See [Git Commit Messages](#git-commit-messages) for the format.

### Code comments: why is this code the way it is?

Explain what the code cannot show by itself: an invariant, an ordering that
matters, a reason something cannot be simplified. Do not describe what the
code does or how it got here. When the same explanation applies in several
places, it belongs in `docs/` or in this file instead.
