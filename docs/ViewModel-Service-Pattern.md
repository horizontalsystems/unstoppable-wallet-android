# ViewModel-Service Architecture Pattern

This document describes the architecture pattern used for ViewModels and Services in this codebase. Follow this pattern when creating new features.

## Overview

The pattern separates concerns between:
- **Services**: Handle business logic, validation, and state management for specific domains
- **ViewModel**: Orchestrates services, collects their states, and exposes a unified UI state

```
┌─────────────┐     stateFlow      ┌─────────────┐
│   Service   │ ─────────────────► │  ViewModel  │
└─────────────┘                    │             │
                                   │ - collect   │
┌─────────────┐     stateFlow      │ - handle*   │
│   Service   │ ─────────────────► │ - emitState │
└─────────────┘                    │             │
                                   │ createState │
       ▲                           └──────┬──────┘
       │                                  │
       │ setX() / actions                 │ UiState
       │                                  ▼
       │                           ┌─────────────┐
       └─────────────────────────  │    View     │
              user actions         └─────────────┘
```

---

## Service Pattern

Services extend `ServiceState<T>` and manage a specific domain of state.

### Structure

```kotlin
package io.horizontalsystems.bankwallet.modules.feature

import io.horizontalsystems.bankwallet.core.ServiceState

class FeatureService(
    // Dependencies injected via constructor
    private val dependency: SomeDependency
) : ServiceState<FeatureService.State>() {

    // Private state variables
    private var value: String? = null
    private var error: Throwable? = null

    // Required: Create state from private variables
    override fun createState() = State(
        value = value,
        error = error
    )

    // Public setters - receive input, update state, emit
    fun setValue(newValue: String?) {
        this.value = newValue

        validate()  // Optional: run validation/refresh logic

        emitState()
    }

    // Private logic methods
    private fun validate() {
        error = if (value.isNullOrBlank()) {
            InvalidValueException()
        } else {
            null
        }
    }

    // Nested State data class - exposed to ViewModel
    data class State(
        val value: String?,
        val error: Throwable?
    )
}

class InvalidValueException : Exception()
```

### Key Rules

1. **Extend `ServiceState<Service.State>()`**
2. **Private state variables** - never expose mutable state directly
3. **`createState()`** - maps private state to immutable `State` data class
4. **`setX()` methods** - public entry points that:
   - Update private state
   - Run validation/refresh logic if needed
   - Call `emitState()` at the end
5. **Nested `State` data class** - contains only the data needed by ViewModel

### Example: Real Service

```kotlin
class TokenBalanceService(
    private val adapterManager: IAdapterManager,
) : ServiceState<TokenBalanceService.State>() {

    private var token: Token? = null
    private var balance: BigDecimal? = null
    private var error: Throwable? = null

    override fun createState() = State(
        balance = balance,
        error = error
    )

    fun setToken(token: Token?) {
        this.token = token
        refreshBalance()
        validate()
        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        validate()
        emitState()
    }

    private fun refreshBalance() {
        val adapter = token?.let { adapterManager.getAdapter(it) }
        balance = adapter?.balanceData?.available
    }

    private fun validate() {
        // validation logic
    }

    data class State(
        val balance: BigDecimal?,
        val error: Throwable?
    )
}
```

---

## ViewModel Pattern

ViewModels extend `ViewModelUiState<T>` and orchestrate multiple services.

### Structure

```kotlin
package io.horizontalsystems.bankwallet.modules.feature

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import kotlinx.coroutines.launch

class FeatureViewModel(
    // Services injected via constructor
    private val serviceA: ServiceA,
    private val serviceB: ServiceB,
) : ViewModelUiState<FeatureUiState>() {

    // Local state mirrors service states
    private var stateA = serviceA.stateFlow.value
    private var stateB = serviceB.stateFlow.value

    // Simple properties without logic can live here (exception)
    private var memo: String? = null

    init {
        // Collect each service's stateFlow
        viewModelScope.launch {
            serviceA.stateFlow.collect {
                handleUpdatedStateA(it)
            }
        }
        viewModelScope.launch {
            serviceB.stateFlow.collect {
                handleUpdatedStateB(it)
            }
        }
    }

    // Required: Create UI state from all local states
    override fun createState() = FeatureUiState(
        valueA = stateA.value,
        valueB = stateB.value,
        memo = memo,
        error = stateA.error ?: stateB.error,
        canSubmit = stateA.canSubmit && stateB.canSubmit
    )

    // Public functions - delegate to services
    fun onEnterValueA(value: String?) {
        serviceA.setValue(value)
    }

    fun onEnterValueB(value: String?) {
        serviceB.setValue(value)
    }

    // Exception: Simple property without logic
    fun onEnterMemo(value: String) {
        memo = value.ifBlank { null }
        emitState()
    }

    // Private handlers - update local state, trigger side effects, emit
    private fun handleUpdatedStateA(state: ServiceA.State) {
        this.stateA = state

        // Side effects: notify other services if needed
        serviceB.setDependentValue(state.value)

        emitState()
    }

    private fun handleUpdatedStateB(state: ServiceB.State) {
        this.stateB = state
        emitState()
    }
}

// UI State - single source of truth for the View
data class FeatureUiState(
    val valueA: String?,
    val valueB: String?,
    val memo: String?,
    val error: Throwable?,
    val canSubmit: Boolean
)
```

### Key Rules

1. **Extend `ViewModelUiState<UiState>()`**
2. **Services injected via constructor**
3. **Local state variables** - mirror each service's current state
4. **Collect in `init`** - use `viewModelScope.launch { stateFlow.collect { } }`
5. **Handler pattern** - `handleUpdatedXState(state)`:
   - Update local state
   - Trigger side effects on other services
   - Call `emitState()`
6. **`createState()`** - combines all local state into single `UiState`
7. **Public functions** - delegate to services (e.g., `onEnterX()` → `service.setX()`)
8. **All state in UiState** - avoid separate `mutableStateOf` properties

---

## State Ownership Rules

| Has Logic? | Where to Put State |
|------------|-------------------|
| Yes (validation, async, transformation) | **Service** (default) |
| No (simple pass-through) | **ViewModel** (exception) |

### Example: When to Use Service vs ViewModel

```kotlin
// ✅ Service - has validation logic
class AmountService : ServiceState<State>() {
    private var amount: BigDecimal? = null
    private var error: Throwable? = null

    fun setAmount(value: BigDecimal?) {
        amount = value
        error = if (value != null && value > maxAmount) {
            ExceedsMaxError()
        } else null
        emitState()
    }
}

// ✅ ViewModel - simple pass-through, no logic
class SendViewModel(...) : ViewModelUiState<UiState>() {
    private var memo: String? = null

    fun onEnterMemo(value: String) {
        memo = value.ifBlank { null }
        emitState()
    }
}
```

---

## Collection Pattern

```kotlin
init {
    viewModelScope.launch {
        service.stateFlow.collect {
            handleUpdatedState(it)
        }
    }
}
```

---

## Factory Pattern

Create services in the Module's Factory class:

```kotlin
class FeatureViewModel {

    class Factory(
        private val wallet: Wallet
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            // Create services
            val serviceA = ServiceA(App.dependency1)
            val serviceB = ServiceB(App.dependency2)
            val serviceC = ServiceC()

            // Create and return ViewModel
            return FeatureViewModel(
                serviceA,
                serviceB,
                serviceC,
                // other dependencies
            ) as T
        }
    }
}
```

---

## Checklist for New Features

- [ ] Create Service(s) extending `ServiceState<State>()`
- [ ] Define private state variables in Service
- [ ] Implement `createState()` in Service
- [ ] Add `setX()` methods with `emitState()` at the end
- [ ] Create ViewModel extending `ViewModelUiState<UiState>()`
- [ ] Inject services via constructor
- [ ] Add local state variables mirroring service states
- [ ] Collect service flows in `init` using `viewModelScope.launch`
- [ ] Implement `handleUpdatedXState()` handlers
- [ ] Implement `createState()` combining all states
- [ ] Create Factory in Module to wire dependencies
- [ ] Add all UI-relevant state to `UiState` data class