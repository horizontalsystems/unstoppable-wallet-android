# Hilt Trunk Migration Plan

Plan for migrating the foundational singletons constructed in `App.onCreate()` (the
dependency-injection "trunk") to Hilt. Tracked on branch `hilt-di`.

## Status

**Done:**
- All `@HiltViewModel` / `@AssistedInject` ViewModels and their helpers are clean of `App.*`.
- 14 leaf singletons migrated to `@Inject @Singleton` and removed from `App.onCreate()` (see git history).
- **Phases A–E COMPLETE (2026-06):** every foundational singleton constructed in `App.onCreate()` is
  now Hilt-owned. Construction in `onCreate()` is inverted through per-cluster `@EntryPoint`s (declared
  in `AppInitializer.kt`) that resolve the Hilt instance and assign the `companion` `lateinit var` for
  back-compat. Migrated: LocalStorageManager + storages/DAOs, RestoreSettings, node settings,
  EVM sync sources, account core (AccountManager/UserManager/AccountFactory/WalletManager/CoinManager/
  BackupManager), config leaves (Price/Terms/Connectivity/SystemInfo/AppIcon), Currency/NumberFormatter/
  LanguageManager, AppConfigProvider, BackgroundManager, MarketKitWrapper (factory `@Provides`),
  TorManager, all kit managers (Solana/Tron/Ton/Stellar/Zano + RpcSource/WalletManager),
  Btc/EvmBlockchainManager (+EvmAccountManagerFactory), AdapterFactory/AdapterManager/
  TransactionAdapterManager, Spam/StatsManager, TonConnectManager (factory), PinComponent/PinDbStorage,
  RateAppManager, WCManager/WCSessionManager. Each cluster was cold-start runtime-verified on device.

**Only 5 `App.*`/`CoreApp.*` bridges remain in `AppModule.kt` — all intentionally permanent:**
- `provideKeyStoreManager`, `provideLockoutStorage` → `CoreApp.*` (owned by `core` module).
- `provideMarketFavoritesManager`, `provideMarketWidgetManager` → cycle (see §2).
- `provideAppDatabase` → infra root the DAO providers depend on.

**Remaining work = Phase F + G.** 134 non-Hilt files still read `App.*` directly. These keep the
`companion object` `lateinit var`s (and the `onCreate()` inversion) alive, blocking demolition. Current
hotspots: `numberFormatter` (41), `marketKit` (28), `adapterManager` (22), `appConfigProvider` (17),
`accountManager` (14), `currencyManager` (12), `coinManager` (11), `evmBlockchainManager` (10).

Breakdown of the 134 consumer files: 40 composables, 8 services, 5 `Module.kt` factories, 3 ViewModels,
15 `adapters/*`, 63 objects/util/extensions/managers. Note many of the 63 are the **already-Hilt
managers themselves** still doing *internal* `App.*` reads (e.g. `App.instance` as Context,
`App.appConfigProvider`) that were deliberately left to limit churn — they have an `@Inject` constructor
already, so cleaning them is mechanical.

## Why the trunk is not a mechanical leaf migration

A green `:app:assembleBaseDebug` (which runs Hilt's `hiltJavaCompileBaseDebug` graph validation)
is **necessary but not sufficient** for the trunk. Four hard constraints make build-green ≠ correct:

### 1. Eager startup side-effects
`App.startTasks()` runs a coroutine at launch, in order, that must keep firing:

```
EthereumKit.init()
walletManager.start(restoreSettingsManager, moneroNodeManager, zanoNodeManager,
                    btcBlockchainManager, evmBlockchainManager, solanaKitManager, tronKitManager)
adapterManager.startAdapterManager()
marketKit.sync()
rateAppManager.onAppLaunch()
nftMetadataSyncer.start()
pinComponent.initDefaultPinLevel()
accountManager.clearAccounts()
wcSessionManager.start()
swapSyncService.start()
AppVersionManager(systemInfoManager, localStorage).storeAppVersion()
evmLabelManager.sync()
contactsRepository.initialize()
appIconService.validateAndFixCurrentIcon()
```

Plus eager calls during construction:
`tronAccountManager.start()`, `tonAccountManager.start()`, `stellarAccountManager.start()`,
`tonConnectManager.start()`, `wcManager.addWcHandler(...)` ×2, `DAppManager.initialize(...)`.

Hilt instantiates singletons **lazily** (on first injection). If these move to Hilt without an
explicit eager trigger, the sequence silently never runs — breaking wallet sync, widgets,
WalletConnect, PIN init. The compiler cannot detect this.

### 2. Dependency cycles
`MarketFavoritesManager → MarketWidgetManager → App.marketWidgetRepository → MarketFavoritesManager`
is already worked around by keeping `MarketFavoritesManager` behind a `@Provides` bridge. More
cycles are expected in the adapter layer.

### 3. App-instance-as-Context
- `LanguageManager` calls `App.instance.getLocale()` / `.setLocale()` internally.
- `App.instance` is used as a `Context` throughout.
- Fixed by injecting `@ApplicationContext`.

Note: storage is NOT a problem here. `LocalStorageManager` is already a standalone class
(`ILocalStorage, IPinSettingsStorage, ILockoutStorage, IThirdKeyboard, IMarketStorage`) taking only
`SharedPreferences`. The `localStorage = this` etc. lines in `App.onCreate()` assign the companion
props to that single instance — `App` does not implement the storage interfaces. This makes
`LocalStorageManager` an easy early target (Phase A): `@Inject @Singleton`, provide `SharedPreferences`
via `@ApplicationContext`, bind the five interfaces to the one instance.

### 4. 129 direct `App.*` consumers
Adapters, blockchain helpers, and entity extensions read `App.*` with no injection path yet. The
`companion object` properties must remain until those consumers are migrated.

## Phased plan

Each phase: implement → `:app:assembleBaseDebug` → **runtime launch verification** → commit one
logical batch.

### Phase 0 — Infrastructure prerequisites
- Provide `AppDatabase` and `@ApplicationContext Context` through Hilt (replaces the many
  `appDatabase.xxxDao()` and `App.instance`-as-Context call sites).
- Introduce an `AppInitializer` `@Inject` class that holds the eager startup logic.
  `App.onCreate()` resolves it via an `@EntryPoint` and calls `initialize()`, preserving the exact
  `startTasks()` order while letting Hilt own construction of the singletons it touches.

### Phase A — Storages & DAOs (leaf-most, lowest risk)
`BlockchainSettingsStorage`, `EvmSyncSourceStorage`, `AccountsStorage`, `RestoreSettingsStorage`,
`EnabledWalletsStorage` / `WalletStorage`, `MoneroNodeStorage`, `ZanoNodeStorage`,
`ProFeaturesStorage`, `ScannedTransactionStorage`, and the `xxxDao()` accessors. All take
`AppDatabase` — trivial once Phase 0 provides it.

### Phase B — Config-derived leaves
`PriceManager`, `CurrencyManager`, `NumberFormatter`, `SystemInfoManager`, `ConnectivityManager`,
`FeeRateProvider`, birthday providers, `RestoreSettingsManager`. `LanguageManager` needs
`@ApplicationContext` injected to drop its `App.instance` access. Clears the `numberFormatter`
(40-consumer) hotspot.

### Phase C — Account core
`AccountManager`, `UserManager`, `AccountFactory`, `WalletManager`, `CoinManager`, `BackupManager`,
`EncryptionManager`. Mostly linear deps; `walletManager.start(...)` moves into `AppInitializer`.

### Phase D — Kit & blockchain managers
`EvmSyncSourceManager`, `BtcBlockchainManager`, `MoneroNodeManager`, `ZanoNodeManager`,
`ZanoKitManager`, `SolanaRpcSourceManager`, `SolanaKitManager`, `TronKitManager`, `TonKitManager`,
`StellarKitManager`, `EvmBlockchainManager`. The `xxxAccountManager.start()` calls move to
`AppInitializer`.

### Phase E — Adapter layer (do last among managers)
`AdapterFactory`, `AdapterManager`, `TransactionAdapterManager`, `SpamManager`. Highest coupling
(`adapterManager` = 22 consumers). `startAdapterManager()` → `AppInitializer`.

### Phases A–E — ✅ DONE
All trunk singletons are Hilt-owned (see Status). Construction-inversion via per-cluster `@EntryPoint`
keeps the companions populated for the not-yet-migrated consumers below.

### Phase F — Migrate the 134 direct `App.*` consumers
This is the current frontier. Tackle in risk order:

- **F1 — ✅ DONE (2026-06).** Every already-`@Inject`/`@HiltViewModel`/`@AssistedInject` class that
  read `App.*` internally now takes those deps via the constructor (`@ApplicationContext Context` or
  `Application` for `App.instance`; injected `AppConfigProvider`/`MarketKitWrapper`/`CurrencyManager`/
  `IAppNumberFormatter`/`WalletManager`/`EvmBlockchainManager`/etc.). Cleaned: the 6 kit managers,
  SystemInfoManager, ConnectivityManager, LanguageManager (`LocaleHelper.get/setLocale(context)`),
  AccountManager (`application as CoreApp`), AppIconService, ContactsRepository, the 3 OpenCryptoPay
  VMs, BackupRequiredAlertViewModel. Consumer files: 134 → 118. The only already-Hilt classes still
  referencing `App.*` are `AppModule.kt` (permanent bridges) and `AppInitializer.kt` (startup) — both
  intentional. Does **not** yet allow deleting companions (the F2/F3 globals still read them).
- **F2 — composables (40) reading `App.*`.** Source the value from the screen's `hiltViewModel()`
  VM (or `LocalContext`) instead of the companion. A few (e.g. `App.pinComponent` lock checks) may
  legitimately move into a small VM.
- **F3 — true globals with no injection seam (hardest, may be partial).** `object` singletons
  (`Translator`, `*Helper`), top-level extension functions, entity extensions, and adapter classes
  built by `AdapterFactory`. Options: thread the dependency through call params, have the now-`@Inject`
  `AdapterFactory` pass deps into the adapters it builds, or accept a permanent thin accessor for a
  handful. Realistically a few of these stay companion-backed.

### Phase G — Demolition (gated on F)
Remove each `lateinit var` + its `@EntryPoint` inversion in `onCreate()` only once **all** its
consumers are migrated. `MarketFavorites/MarketWidget` (cycle), `KeyStore/Lockout` (CoreApp), and
`AppDatabase` bridges are expected to remain. End state: `App` is `@HiltAndroidApp` + the
`AppInitializer` eager-startup call, with a minimal companion (or none).

## Special-case fixes
- `LocalStorageManager` (backs `ILocalStorage`, `IPinSettingsStorage`, `ILockoutStorage`,
  `IThirdKeyboard`, `IMarketStorage`) → `@Inject @Singleton`; provide `SharedPreferences` via
  `@ApplicationContext`; `@Binds`/`@Provides` the five interfaces to the single instance.
- `LanguageManager` / `App.instance`-as-Context → inject `@ApplicationContext`.
- Cycles → `dagger.Lazy<T>` / `Provider<T>` at the back-edge.
- Eager singletons → referenced from `AppInitializer` so they are instantiated at launch, not lazily.

## Verification gate per phase
`:app:assembleBaseDebug` (Hilt graph validation) **and** a runtime launch confirming: wallet sync
starts, balances load, widgets update, WalletConnect handlers register, PIN init runs, app version
stored. A green build alone does not certify a phase.
