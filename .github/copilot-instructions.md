# Quantum Wallet — Project Instructions

## Identity

- **App Name**: Quantum Wallet
- **Package**: `com.quantum.wallet.bankwallet`
- **Developer**: Quantum Chain PTE Ltd.
- **Platform**: Android (Kotlin, Jetpack Compose)

## Architecture

See [CLAUDE.md](../CLAUDE.md) for detailed architecture, patterns, and build commands.

Key points:
- **Language**: Kotlin · **UI**: Jetpack Compose (new code in `uiv3/`, legacy in `ui/`)
- **Reactive**: Kotlin Coroutines + StateFlow. Do NOT use RxJava in new code.
- **DI**: Manual via `App` companion object
- **Pattern**: MVVM with `ViewModelUiState<T>` base class and `ServiceState<T>` for business logic
- **Modules**: `:app`, `:core`, `:components:icons`, `:components:chartview`, `:subscriptions-core`, `:subscriptions-google-play`, `:subscriptions-fdroid`, `:subscriptions-dev`

## Rebranding Context

This project is being rebranded from "Unstoppable Wallet" by Horizontal Systems to "Quantum Wallet" by Quantum Chain PTE Ltd.

### Package Mapping

| Old | New |
|-----|-----|
| `io.horizontalsystems.bankwallet` | `com.quantum.wallet.bankwallet` |
| `io.horizontalsystems.core` | `com.quantum.wallet.core` |
| `io.horizontalsystems.chartview` | `com.quantum.wallet.chartview` |
| `io.horizontalsystems.icons` | `com.quantum.wallet.icons` |
| `io.horizontalsystems.subscriptions.core` | `com.quantum.wallet.subscriptions.core` |
| `io.horizontalsystems.subscriptions.dev` | `com.quantum.wallet.subscriptions.dev` |
| `io.horizontalsystems.subscriptions.fdroid` | `com.quantum.wallet.subscriptions.fdroid` |
| `io.horizontalsystems.subscriptions.googleplay` | `com.quantum.wallet.subscriptions.googleplay` |

### Brand Mapping

| Old | New |
|-----|-----|
| Unstoppable | Quantum Wallet |
| Horizontal Systems | Quantum Chain PTE Ltd. |
| Deeplink scheme `unstoppable` | `quantum` |

### External Dependencies — DO NOT RENAME

Imports from external HorizontalSystems blockchain kits must stay as-is:
`io.horizontalsystems.bitcoinkit`, `io.horizontalsystems.ethereumkit`, `io.horizontalsystems.solanakit`, `io.horizontalsystems.tronkit`, `io.horizontalsystems.tonkit`, `io.horizontalsystems.zcashbinancekt`, `io.horizontalsystems.monerokit`, `io.horizontalsystems.stellarkit`, `io.horizontalsystems.marketkit`, `io.horizontalsystems.feeratekit`, and any other third-party kit dependency.

## Git Rules

- **NEVER** run `git push` in any form
- **NEVER** commit directly to `master` or `main` — always use feature branches
- Use conventional commits: `type(module): description`
- See the git-workflow instruction for full details

## Conventions

- New Compose components go in `uiv3/components/`
- Use `StateFlow` / `SharedFlow`, not RxJava
- State classes: suffix `State` or `UiState`
- ViewModels: suffix `ViewModel`; Services: suffix `Service`
- Prevent double-clicks with local composable state, not ViewModel state
