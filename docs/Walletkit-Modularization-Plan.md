# Walletkit Modularization: Chain Dispatch Map & Extraction Plan

Goal: split the `walletkit` monolith into per-chain Gradle modules so consuming apps
(Unstoppable = all chains, Seya = a small subset) include only the chains they need —
code **and** the heavy kit dependencies (`bitcoin-kit`, `monero-kit`, Zcash SDK, …),
which are all `api` dependencies of `walletkit` today.

All paths are relative to `walletkit/src/main/java/io/horizontalsystems/walletkit/`.

---

## Part 1 — The dispatch map

### 1.0 The structural constraint

`BlockchainType` is a sealed class in the external **market-kit** library. Chain modules
cannot extend it, so the plugin system must be **keyed by** `BlockchainType`
(`Map<BlockchainType, ChainPlugin>`), registered at app assembly. Every exhaustive
`when (blockchainType)` we replace with a registry lookup loses compile-time
exhaustiveness — compensate with a **startup assertion**: every entry in the app's
supported-chain list must have a registered plugin (and vice versa).

### 1.1 Master registries (the two lists everything orbits)

| What | Where |
|---|---|
| `BlockchainType.Companion.supported` — hardcoded list of 23 chains | `core/MarketKitExtensions.kt:646` |
| `blockchainOrderMap` / `BlockchainType.order` — display ordering | `core/MarketKitExtensions.kt:171–204` |

`core/MarketKitExtensions.kt` (677 lines) is effectively the existing "plugin descriptor,"
implemented as 14 `when`-extension properties: `protocolType`, `isSupported` (token types
per chain), `description`, `restoreSettingTypes`, `order`, `tokenIconPlaceholder`, `title`,
`supportedNftTypes`, `brandColor`, `feePriceScale`, `chainId`, `isEvm`,
**`supports(accountType)`** (~80-line matrix of 13 account types × 23 chains — the densest
chain knowledge in the codebase), `nativeTokenQueries`, `defaultTokenQuery`, `badge` rules.

### 1.2 Core-layer dispatch points

| # | Dispatch point | Location | Mechanism |
|---|---|---|---|
| C1 | **`AdapterFactory.getAdapter`** — the biggest single dispatch; nested `when (tokenType)` × `when (blockchainType)` builds every `IAdapter`. Constructor injects 11 chain managers. Includes Monero deferred-creation rule (`isResolvingFastestNode` → null). Also per-chain `xxxTransactionsAdapter()` builders and two near-duplicate `unlinkAdapter` blocks (inconsistent EVM sets; Zano only in one). | `core/factories/AdapterFactory.kt:59–383` | when + DI fan-in |
| C2 | **`TransactionAdapterManager`** — `when (blockchainType)` → tx adapter; `else` casts balance adapter (BTC-family/Zcash/Monero/Zano). Already has one plugin seam (`TransactionsAdapterDecoratorResolver`). | `core/managers/TransactionAdapterManager.kt:52–81` | when |
| C3 | `AddressValidatorFactory` — exhaustive `when` → per-chain validator; reaches `App.adapterManager` statically. | `core/factories/AddressValidatorFactory.kt:20–77` | when |
| C4 | `BlockchainType.uriScheme` / `removeScheme` extensions (no actual factory class). Consumed by `AddressUriParser`, `AddressUri`, receive flow. | `core/factories/AddressParserFactory.kt:6–51` | when |
| C5 | `FeeRateProviderFactory` — BTC-family only, `else -> null`. | `core/factories/FeeRateProviderFactory.kt:16–23` | when |
| C6 | `WalletManager.start(...)` — signature hardcodes 9 chain managers; one coroutine per chain-settings flow → `reloadWallets(type)`. | `core/managers/WalletManager.kt:96–155` | DI fan-in + per-chain flows |
| C7 | `RestoreSettingsManager` + `BirthdayHeightHelper` — Zcash/Monero/Zano birthday-height logic (4 parallel `when` blocks, `else -> throw`). | `core/managers/RestoreSettingsManager.kt:62–82`, `core/managers/BirthdayHeightHelper.kt` | when |
| C8 | `BtcBlockchainManager` — hardcoded family lists (`blockchainTypes`, blockchair-enabled). Already family-manager shaped. | `core/managers/BtcBlockchainManager.kt:24–34` | lists |
| C9 | `EvmBlockchainManager` — `getChain()` `when` over 10 EVM chains; `companion.blockchainTypes` is the de-facto EVM registry (used by `AddressParserFactory`, `AddressChecker`). | `core/managers/EvmBlockchainManager.kt:43–87` | when + list |
| C10 | `EvmSyncSourceManager` — hardcoded default RPC/explorer lists per EVM chain + a Tron shim. Pure config data — clearest manifest candidate. | `core/managers/EvmSyncSourceManager.kt:31–240` | when |
| C11 | `BlockchainSettingsStorage` — chain-pinned methods (`moneroNodeHost`, `zanoNodeHost`, `zcashEndpointUrl`, …) over an **already-generic DAO** (`blockchainUid` + key + value). Thin refactor. | `core/storage/BlockchainSettingsStorage.kt` | hardcoded keys |
| C12 | **`core/Interfaces.kt`** — imports 8 chain SDKs. Per-chain send sub-interfaces (`ISendBitcoinAdapter`, `ISendTronAdapter`, …, `IMoneroAccountsAdapter`); chain-typed members on shared types (`ITransactionsAdapter.getTronFullTransactionsBefore`, `BalanceData.stellarAssets`/`unshielded`, Zcash/BTC members on `IReceiveAdapter`, chain fields on `ILocalStorage`). | `core/Interfaces.kt` | interface pollution |
| C13 | **`App.kt`** — constructs ~25 chain-specific singletons (kit managers, node managers, birthday providers, 4 imperative `*AccountManager.start()` calls — asymmetric vs. EVM's lazy creation). Leak sites for "chain-local" managers are consistently: App construction, `WalletManager.start`, `BackupProvider`. | `core/App.kt:150–430, 509–549, 648–681` | DI wiring |
| C14 | Misc: `AccountCleaner` (static `XxxAdapter.clear` calls; Litecoin/Ton/Stellar/Thorchain/Zano missing), `SpamManager` (per-adapter-class event extractors), `AddressChecker`/`Eip20AddressValidator`/`HashDitAddressValidator` (hardcoded chain lists), `NftMetadataManager` (Ethereum→OpenSea), `BaseTokenManager`, `NftAdapterManager` (EVM-only), `BlockchainType.blockTime` (`core/Extensions.kt:132`, has a dead duplicate ArbitrumOne branch). | various | mixed |

### 1.3 Feature-layer dispatch points

| # | Dispatch point | Location | Notes |
|---|---|---|---|
| F1 | **`SendPage`** — `when (blockchainType)` → per-chain send screen/VM; imports ~30 chain symbols; `else -> {}` silent blank screen. Per-chain send subtrees under `modules/send/{bitcoin,evm,monero,…}/` are already physically separated. | `modules/send/SendPage.kt:68–233` | biggest screen router |
| F2 | `ReceivePage` — branches for Stellar asset / Monero; generic `ReceiveScreen` is itself Bitcoin-flavored (used-addresses row → `BtcUsedAddressesPage`). `ReceiveTokenSelectViewModel` special-cases Zcash address types and Zcash/Monero birthday height. | `modules/receive/ReceivePage.kt:38–73`, `viewmodels/ReceiveTokenSelectViewModel.kt:128–202` | |
| F3 | **Address handlers** — `AddressHandlerFactory.parserChainHandlers` (exhaustive when, imports chain-SDK `MainNet*` classes) and `AddressInputModule` — a **near-duplicate** that must be kept in sync by hand. 16 handler classes in one file (`IAddressHandler.kt`). Chain `Address` subclasses (`BitcoinAddress`, `MoneroWatchAddress`) are `@Serializable` and cross navigation → polymorphic serializer registration needed after split. | `modules/address/AddressHandlerFactory.kt:19–126`, `AddressInputModule.kt:26–101`, `IAddressHandler.kt`, `entities/Address.kt` | dedupe first |
| F4 | **`TransactionViewItemFactory`** — `when (record)` over ~30 `TransactionRecord` subtypes, **order-sensitive** (base is abstract, not sealed). 30 private builders; nested Stellar/Ton sub-dispatches. | `modules/transactions/TransactionViewItemFactory.kt:215–1472` | |
| F5 | **`TransactionInfoViewItemFactory`** — same ~30-subtype `when` for the detail screen; emits the shared sealed `TransactionInfoViewItem` vocabulary (the natural SPI boundary — plugins emit items, shared renderer draws). Fee extraction is a third parallel `when` (`TransactionViewItemFactoryHelper.getStatusSectionItems:613–729`). `resendable` flag at `TransactionInfoModule.kt:66–71`. `TransactionInfoCells.openTransactionOptionsModule:711–758` navigates directly to `ResendBitcoinPage` / `TransactionSpeedUpCancelPage`. | `modules/transactionInfo/` | |
| F6 | `TransactionRecordRepository.groupWalletsBySource` — per-token pools (BTC-family + Zcash) vs. account-wide pools (rest). Cleanest small SPI: one boolean. | `modules/transactions/TransactionRecordRepository.kt:64–111` | easiest win |
| F7 | **Backup** — `BackupLocalModule`: 3 parallel `when (accountType)` codec blocks (axis is AccountType, not chain). `BackupProvider`: 10 injected chain managers, per-chain DTOs with `@SerializedName` wire-format contract, mirror restore/create blocks, Tron smuggled as EVM sync source. | `modules/backuplocal/` | wire format = compatibility contract |
| F8 | Key screens — `PrivateKeysViewModel` / `PublicKeysViewModel` produce **fixed structs with one field per chain** (evm/tron/bip32/stellar/monero). | `modules/manageaccount/…:29–98` | |
| F9 | **Multiswap send services** — `SendTransactionServiceFactory` (exhaustive when → per-chain service; clean one-method extraction) and `SendTransactionData` — a **sealed class with one variant per chain family importing 4 chain SDKs**; sealed-across-modules problem. Swap providers are the heaviest chain-aware files in the module (`USwapProvider` 75 refs, `BaseThorChainProvider` 34, `SwapHelper` 25, `AllBridgeProvider` 25, `SwapInfoViewModel` 25). | `modules/multiswap/sendtransaction/` | hardest type problem |
| F10 | Blockchain settings — sealed `BlockchainItem` per family; `BlockchainSettingsPage` `when` navigates to 7 directly-imported per-chain pages; the 7 per-chain settings dirs (`btcblockchainsettings/`, `evmnetwork/`, `moneronetwork/`, `zanonetwork/`, `zcashnetwork/`, `solananetwork/`, `thorchainnetwork/`) are self-contained and move cleanly. | `modules/blockchainsettings/` | |
| F11 | `AppStatusViewModel` — two near-clone methods each with a hand-written per-chain lookup sequence; 9 injected kit managers. | `modules/settings/appstatus/AppStatusViewModel.kt:174–340` | |
| F12 | Watch accounts — `WatchAddressService.tokens` (`when (accountType)` gated by `supports()`); `WatchAddressViewModel.Type` — a **parallel per-chain enum** + 3 `when`s over it; Monero needs viewKey + birthdayHeight. | `modules/watchaddress/` | |
| F13 | Restore — `RestoreViewModel` (Tron/Stellar single-coin path), `RestorePrivateKeyViewModel.accountTypes` (credential sniffing: hex→EVM/Tron, Stellar seed, xpub), `RestoreSettingsService.enter` (Zcash/Monero birthday — **exact duplicate** of `ReceiveTokenSelectViewModel:198`; Zano declares `BirthdayHeight` but has no branch — latent bug). | `modules/restoreaccount/`, `modules/enablecoin/restoresettings/` | |
| F14 | Balance — `BalanceViewItemFactory` (sync-with-progress chain set, default progress, per-chain badge icons, Zcash locked-row suppression, per-chain `LockedValue` subclasses, `AttentionIconType.TronNotActive` baked into shared enum; duplicate Solana branch smell); `TokenBalanceScreen` (Zcash address-type button, `MoneroAccountCell`, Stellar/Zcash locked bottom sheets, Tron-inactive alert; direct imports of 3 chain pages); `TokenBalanceViewModel` (Monero watch warning, Zcash shield threshold, Tron receive wallet). | `modules/balance/` | |
| F15 | OpenCryptoPay — provider-method-string → chain map; Bitcoin proof path; `buildSendData` `when` → `SendTransactionData.*`. | `modules/opencryptopay/` | |
| F16 | **Navigation: there is no route registry.** `HSPage` self-renders (`GetContent`); navigation is by concrete `@Serializable` page object → every cross-module navigation is a compile-time dependency. Known hard edges: `BlockchainSettingsPage`→7 chain pages, `SendPage`→10, `TokenBalanceScreen`→3, `ReceivePage`→3, `TransactionInfoCells`→2. Pages moving to chain modules also require kotlinx.serialization polymorphic registration → a per-plugin `SerializersModule`/page registry solves both. | `modules/nav3/` | must build the seam |
| F17 | Misc: `AddTokenService` (custom-token chain list + Tron/Ton/Solana resolvers), `MarketFiltersService` (20 refs), contacts `AddressViewModel`, `AddressUri.Field.amountField` (Monero), `EnterAddressViewModel` Zano alias, `SwapPopularTokens`, Ton/Stellar transaction records living in `core/adapters/` instead of `entities/transactionrecords/` (inconsistency to normalize). | various | |

### 1.4 Known latent bugs / inconsistencies found during mapping

1. `AdapterFactory.unlinkAdapter(wallet)` vs `unlinkAdapter(transactionSource)` enumerate different EVM sets; Zano appears only in the wallet variant (`AdapterFactory.kt:320–383`).
2. `AccountCleaner` misses Litecoin/Ton/Stellar/Thorchain/Zano (`core/managers/AccountCleaner.kt:21–31`).
3. Zano declares `BirthdayHeight` restore setting but `RestoreSettingsService.enter`/`ReceiveTokenSelectViewModel` return null for it.
4. `core/Extensions.kt:132` `blockTime`: dead duplicate `ArbitrumOne` branch (first match wins).
5. `BalanceViewItemFactory.getDefaultSyncingProgress` has a duplicate `Solana` branch.
6. `SendPage`'s `else -> {}` renders a silent blank screen for unhandled chains.
7. `uriScheme` is missing Thorchain.

---

## Part 2 — Target architecture

### 2.1 Module layout

```
:walletkit-core        ← market-kit, storage/Room, accounts, ChainRegistry + ChainPlugin SPI,
                          shared UI (nav3, compose components, TransactionInfoViewItem vocabulary),
                          generic screens (balance list, transactions list, settings hub, backup engine)
:walletkit-chain-btc   ← bitcoin/bch/dash/ecash/litecoin kits + adapters + send/receive/settings UI
:walletkit-chain-evm   ← ethereum/erc20/uniswap/oneinch/nft kits, web3j
:walletkit-chain-tron
:walletkit-chain-solana
:walletkit-chain-ton
:walletkit-chain-stellar
:walletkit-chain-monero
:walletkit-chain-zano
:walletkit-chain-zcash
:walletkit-chain-thorchain   (mostly multiswap provider — may fold into a swap module instead)
:walletkit             ← thin umbrella (backwards compat): depends on core + all chains (Unstoppable)
```

Each chain module: `api(libs.kit.x)` moves out of walletkit's build file into its module.
Seya then depends on `:walletkit-core` + the chain modules it wants; monero-kit, Zcash SDK
(native `.so` libs), bitcoin kits, Tor, etc. disappear from its dependency graph and APK.

Dependency direction: `chain modules → core`. Core never references a chain module.
Apps (`:app`, Seya) depend on core + chosen chains and register plugins at startup.

### 2.2 The `ChainPlugin` SPI (synthesis of both maps)

```kotlin
interface ChainPlugin {
    val blockchainType: BlockchainType
    val order: Int

    // Metadata (replaces core/MarketKitExtensions.kt + Extensions.kt entries)
    val title: String;  val description: String
    val uriScheme: String?;  val removeUriScheme: Boolean;  val amountUriField: AddressUri.Field
    val tokenIconPlaceholder: Int?;  val chainBadgeIcon: Int?;  val brandColor: Color?
    val blockTime: Long?;  val feePriceScale: FeePriceScale;  val chainId: Long?  // EVM only
    val restoreSettingTypes: List<RestoreSettingType>
    fun supportedTokenTypes(tokenType: TokenType): Boolean       // isSupported
    fun nativeTokenQueries(): List<TokenQuery>;  fun defaultTokenQuery(): TokenQuery
    fun protocolType(tokenType: TokenType): String?
    fun supports(accountType: AccountType): Boolean              // the 80-line matrix, co-located with kit manager

    // Adapters & lifecycle (C1, C2, C6, C13, C14)
    fun createAdapter(wallet: Wallet, deps: CoreDeps): IAdapter?         // null = deferred (Monero)
    fun createTransactionsAdapter(source: TransactionSource): ITransactionsAdapter?  // null = reuse balance adapter
    fun unlink(account: Account)
    fun onAppStart()                              // replaces imperative *AccountManager.start() in App
    val walletReloadTrigger: Flow<Unit>?          // replaces WalletManager.start fan-in
    fun clearAccountData(accountId: String)       // replaces AccountCleaner statics
    fun statusInfo(activeWallets: List<Wallet>): Map<String, Any>?   // AppStatus

    // Address (C3, C4, F3)
    fun addressValidator(token: Token): AddressValidator
    fun addressHandlers(): List<IAddressHandler>
    fun domainHandlers(): List<IAddressHandler>
    fun feeRateProvider(): IFeeRateProvider?

    // Screens & navigation (F1, F2, F10, F16)
    @Composable fun SendScreen(args: SendArgs)
    @Composable fun ReceiveScreen(args: ReceiveArgs)?     // null → generic screen
    fun settingsRow(): BlockchainSettingsRow?             // subtitle, group, target page, stat event
    fun resendPage(type: ResendType, record: TransactionRecord): HSPage?
    @Composable fun TokenBalanceExtras(wallet: Wallet)    // Monero accounts cell, Zcash shield, Tron alert
    val serializersModule: SerializersModule              // polymorphic HSPage + Address registration

    // Transactions (F4, F5, F6)
    val transactionsGroupedPerToken: Boolean
    val resendable: Boolean
    fun listViewItem(record: TransactionRecord, ctx: ListItemCtx): TransactionViewItem?
    fun infoSections(record: TransactionRecord, ctx: InfoCtx): List<List<TransactionInfoViewItem>>?
    fun fee(record: TransactionRecord): TransactionValue?

    // Balance (F14)
    val syncsWithProgress: Boolean;  val defaultSyncProgress: Int
    fun lockedValues(balanceData: BalanceData, wallet: Wallet): List<LockedValue>

    // Accounts / restore / watch (C7, F8, F12, F13)
    fun newWalletBirthdayHeight(): Long?
    fun birthdayHeightSupport(): BirthdayHeightSupport?    // estimate height↔date, min height, first block
    fun watchAccountSupport(): WatchSupport?               // required fields, AccountType builder
    fun parsePrivateKeyCredential(text: String): AccountType?
    fun privateKeyItems(account: Account): List<KeyDisplayItem>
    fun publicKeyItems(account: Account): List<KeyDisplayItem>

    // Infra (C10, C11, F7, F9, F15)
    fun sendTransactionService(token: Token): AbstractSendTransactionService
    fun exportSettings(): Pair<String, JsonElement>?       // stable @SerializedName key preserved
    fun importSettings(json: JsonElement)
    val ocpMethodName: String?
}
```

Not everything must land in v1 — the interface can grow hook by hook as dispatch points
are converted (Phase 1 below). `BalanceData`'s chain fields (`stellarAssets`, `unshielded`)
and `ITransactionsAdapter`'s chain-typed methods need type surgery (opaque extras map /
capability sub-interfaces) rather than SPI hooks.

### 2.3 Hard problems and their resolutions

1. **Sealed classes across modules** (`SendTransactionData`, `BlockchainItem`): convert to
   non-sealed interfaces in core, or keep chain-specific payloads opaque (`Any` payload the
   owning plugin downcasts). Recommended: interface + plugin-owned data classes.
2. **Exhaustiveness loss**: startup assertion `ChainRegistry.validate(appSupportedChains)`,
   plus fail-fast lookups (`error("no plugin for $type")` instead of silent `else -> {}`).
3. **Navigation coupling** (F16): per-plugin `SerializersModule` merged at app assembly;
   navigation via pages returned by plugin hooks (e.g. `settingsRow().page`), never
   imported directly by shared screens.
4. **`@Serializable` polymorphism** for `Address` subclasses and `HSPage` subclasses in
   chain modules — same `SerializersModule` mechanism.
5. **Backup wire format** (F7): the JSON keys (`btc_modes`, `monero_nodes`, …) are a
   compatibility contract. Keep the `Settings` DTO in core as `Map<String, JsonElement>`
   with plugins owning their key; add a round-trip test against a fixture backup file
   from the current release before refactoring.
6. **`core/Interfaces.kt` SDK imports** (C12): move `ISendXxxAdapter` interfaces into their
   chain modules; shared code only knows `IAdapter`/`ISendAdapter` base + capability lookup.
7. **BTC family granularity**: keep Bitcoin/BCH/Dash/ECash/Litecoin as ONE `chain-btc`
   module (they share bitcoincore, handlers, send UI, `BtcBlockchainManager`); splitting
   further has poor cost/benefit.
8. **Resources**: per-chain strings/drawables (`logo_chain_*`, chain settings screens' strings)
   move with their module; Android library resource merging handles the rest.

---

## Part 3 — Phased plan

### Phase 0 — Prep & dedup (small PRs, zero behavior change)

1. Unify `AddressHandlerFactory.parserChainHandlers` and `AddressInputModule` duplication (F3).
2. Dedupe birthday-height logic (`ReceiveTokenSelectViewModel:198` vs `RestoreSettingsService:56`); fix the Zano gap (bug #3).
3. Collapse `AppStatusViewModel`'s two near-clone methods (F11).
4. Normalize Ton/Stellar transaction records out of `core/adapters/` into `entities/transactionrecords/` (F17).
5. Fix the `unlinkAdapter` inconsistency and `AccountCleaner` gaps (bugs #1, #2) — or at least document them as intentional.
6. Add the backup round-trip fixture test (guards Phase 1 backup work).

### Phase 1 — Build the seam in place (still one module)

Create `ChainPlugin` + `ChainRegistry` inside walletkit; implement one plugin object per
chain **in the same module**; convert dispatch points to registry lookups one by one,
easiest → hardest by blast radius:

1. `transactionsGroupedPerToken` (F6) — one boolean, tiny blast radius.
2. `resendable` (F5) and balance sync flags/badges (F14, the flags part).
3. `statusInfo` (F11), `clearAccountData` (C14), `ocpMethodName` (F15).
4. Birthday-height cluster (C7): `newWalletBirthdayHeight`, `birthdayHeightSupport`.
5. `sendTransactionService` factory (F9, factory part only).
6. Metadata block: replace `MarketKitExtensions` extensions with registry-backed lookups; replace `BlockchainType.supported` with `ChainRegistry.supported` (**the keystone — do it here, everything after gets easier**).
7. Address: `addressValidator`, unified `addressHandlers`/`domainHandlers`, `uriScheme` (C3, C4, F3).
8. Adapters: `createAdapter`/`createTransactionsAdapter`/`unlink` (C1, C2); dissolve `AdapterFactory`'s 11-manager constructor; move `AccountType→kit` credential code fully behind plugins.
9. Lifecycle: `onAppStart`, `walletReloadTrigger` (C6, C13) — `WalletManager.start` loses its 9-manager signature; `App.kt` shrinks to `ChainRegistry.register(...)` calls.
10. Screens: `SendScreen` (F1), `ReceiveScreen` (F2), `settingsRow` (F10), `resendPage` + `TokenBalanceExtras` (F5/F14); introduce per-plugin `SerializersModule`.
11. Transactions: `listViewItem` / `infoSections` / `fee` (F4, F5) — mechanical but large; move the 30 builders into their plugins.
12. Backup (F7), watch/restore (F12, F13), key screens (F8) — the AccountType-axis hooks.

After each step: full build of `:app`, run unit tests, manual smoke of the affected screen.
The app still ships identical behavior — this whole phase is refactoring with the
startup assertion keeping the registry honest.

### Phase 2 — Pilot extraction: Zano, then Monero

**Zano first** (smallest surface: 10 kit-importing files, self-contained node manager,
no watch support, no NFT/swap entanglement): create `:walletkit-chain-zano`, move
`ZanoAdapter`/`ZanoKitManager`/`ZanoNodeManager`/`zanonetwork/`/`send/zano`/
`entities/transactionrecords/zano` + `ZanoAliasResolver` + plugin object; move
`api(libs.kit.zano)` into the module. Gate: `:app` builds with the module included AND
walletkit-core builds with it excluded.

**Monero second** — exercises the harder capabilities Zano doesn't: deferred adapter
creation, subaddresses/accounts UI (`TokenBalanceExtras`), watch accounts with extra
fields, `moneroActiveAccount` in `ILocalStorage` (move behind plugin-owned storage),
`autoSelectFastestNodeOnStartup` lifecycle hook.

Deliverable of this phase: a written "how to extract a chain" checklist derived from doing it twice.

### Phase 3 — Remaining chains, in dependency order

1. **Tron** — first chain Seya *keeps*: proves the included-module path end-to-end (TRC20, account activation warning, multiswap Tron service).
2. **Zcash** (shield flow, address types, unshielded balance — type surgery on `BalanceData`).
3. **Bitcoin family** as one module (fee rates, UTXO expert mode, RBF/resend, plugins/hodler).
4. **Solana / Ton / Stellar** (each moves its token auto-enable AccountManager; Ton brings tonconnect coupling, Stellar brings trustline/locked-value types).
5. **EVM last** — biggest surface, and multiswap/walletconnect/nft/addtoken lean on it. By now every seam it needs already exists.
6. Decide Thorchain's home (own module vs. swap module).

### Phase 4 — Consumer wiring & release

1. `:walletkit` becomes a thin umbrella depending on core + all chain modules (Unstoppable unchanged, one-line migration).
2. Seya: depend on `:walletkit-core` + chosen chain modules; register only those plugins; measure APK delta (expect large win from dropping monero-kit/Zcash native libs, bitcoin kits, Tor if also modularized).
3. Optional follow-ups on the same pattern: `tor` module (already isolated in `core/tor` + `modules/tor`), `market`/coin-page module (Seya's original exclusion wish), walletconnect (partly done via `dapp-*`).

---

## Verification strategy (applies to every phase)

- Startup assertion: registry completeness vs. app's declared chain set.
- Backup fixture round-trip test (created in Phase 0) must pass unmodified.
- Per-phase gates: `./gradlew :app:assembleDebug` + unit tests; screen smoke for send /
  receive / balance / transactions / settings / backup-restore of the touched chain.
- After each chain extraction: build matrix — app **with** the module and a test consumer
  **without** it (catches lingering compile references immediately).
