# CLAUDE.md

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

- **Repositories**: RxJava `Observable` / `BehaviorSubject`
- **ViewModels**: Kotlin `StateFlow`
- **Conversion**: Use `.asFlow()` to convert RxJava to Flow

```kotlin
// In ViewModel
viewModelScope.launch {
    repository.itemsObservable.asFlow().collect { items ->
        // handle
    }
}
```

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

## Common Gotchas

1. **RxJava to Flow**: If Observable completes, Flow stops collecting silently
2. **Coroutine exceptions**: Uncaught exceptions in `launch` blocks terminate silently
3. **Mutex in Services**: Use `mutex.withLock` for thread-safe state updates
4. **ViewModel cleanup**: Call `service.clear()` in `onCleared()`

## Testing

- Unit tests in `app/src/test/`
- Instrumentation tests in `app/src/androidTest/`
