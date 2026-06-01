# Hilt Trunk Migration Plan

Plan for migrating the foundational singletons constructed in `App.onCreate()` (the
dependency-injection "trunk") to Hilt. Tracked on branch `hilt-di`.

## Status

**Done:**
- All `@HiltViewModel` / `@AssistedInject` ViewModels and their helpers are clean of `App.*`.
- 14 leaf singletons migrated to `@Inject @Singleton` and removed from `App.onCreate()`:
  `PassphraseValidator`, `BackupViewItemFactory`, `DonationShowManager`, `ReleaseNotesManager`,
  `WordsManager`, `RecentAddressManager`, `BalanceViewTypeManager`, `BalanceHiddenManager`,
  `BaseTokenManager`, `BackupProvider`, `RoiManager`, `WalletActivator`, `SwapTermsManager`,
  `PaidActionSettingsManager`.

**Remaining (the trunk):** ~66 `@Provides` `App.*` bridges in `AppModule.kt`, and 129 non-Hilt
files still reading `App.*` directly. Top consumers: `numberFormatter` (40), `marketKit` (27),
`adapterManager` (22), `appConfigProvider` (20), `accountManager` (14), `coinManager` (11),
`localStorage` (10), `currencyManager` (10).

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

### Phase F — Migrate the 129 direct consumers
Adapters / helpers / entity-extensions: convert to `@Inject` where Hilt-reachable, or thread
dependencies through. Only after this can the companion props and bridges be deleted wholesale.

### Phase G — Demolition
Remove the `lateinit var`s, `AppModule` bridges, and `App.onCreate()` construction. `App` becomes
just `@HiltAndroidApp` plus the `AppInitializer` call.

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
