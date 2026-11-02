# Litecoin MWEB: design and delta matrix

Issue: horizontalsystems/unstoppable-wallet-android#9622 (Add LMWEB Support).
Analog: Litecoin (bitcoin family, `walletkit-chain-bitcoin`). MWEB is its own
blockchain in the app model, `BlockchainType.LitecoinMweb` (uid
`litecoin-mweb`, title "Litecoin MWEB"), holding the coin `litecoin` (LTC) as
its single native token, the way Zano is its own chain. The user sees one more
wallet row "LTC · Litecoin MWEB" next to the Litecoin derivation rows.

Kit: `litecoinmwebkit` in bitcoin-kit-android, branch `litecoin-mweb`
(commit `bd18aee0` at the time of writing; the app pins that hash in
`gradle/libs.versions.toml`). Module README: `litecoinmwebkit/README.md`.
Measurements that drove the kit decisions: [spike.md](spike.md).

## What MWEB changes, in one paragraph

MWEB coins live in an extension block attached to every Litecoin block.
Amounts and addresses are hidden. Coins enter with a peg-in (a Litecoin
transaction whose output is `OP_9 <kernel hash>` and which carries the MWEB
body), leave with a peg-out (an MWEB kernel paying a Litecoin script; the
coins land in the block's HogEx transaction and are spendable after 6
confirmations), and move inside MWEB with kernel-only transactions that have
no Litecoin txid. A wallet is a scan key and a spend key (`m/1000'/0'`,
`m/1000'/1'`, the scheme Cake Wallet uses); addresses are
`ltcmweb1…` (117 characters), one per index, index 0 reserved for change. The
light client only serves the current unspent set: a restore recovers the
exact balance and the unspent incoming outputs, never spent outputs or sends.

## Decisions

| # | Decision | Reason |
|---|---|---|
| D1 | Engine is the embedded `mwebd` daemon (Go, over loopback gRPC), one daemon per process, chain data shared by all MWEB wallets on the device in `<filesDir>/mweb/<network>`. | The only maintained MWEB light client; Cake, Stack, Litewallet and Electrum-LTC use it. One header chain per device, not per wallet ([spike.md](spike.md) recommendation b). |
| D2 | First sync starts from a header checkpoint bundled in the kit; the genesis path stays as fallback. | Without it the first sync is 400 MB on the wire, 700 MB on disk and 30 min on a phone; with it, seconds and about 100 MB ([spike.md](spike.md) recommendation a). |
| D3 | MWEB is a new blockchain `litecoin-mweb` with one `TokenType.Native` token of the coin `litecoin`; no new coin, no new token type. The Litecoin chain keeps its four derivations and BIP84 default. | The chain is the unit the app is modularised around (`ChainPlugin`, `ChainRegistry`): adapter, address handlers, validator, send service, cleanup and settings all hang off it, and swap and private-send providers enumerate chains, so MWEB is excluded without a filter. A new token type would instead touch every exhaustive `when` on `TokenType` and the backup id format. MWEB has no token layer (no asset ids in LIP-0002/3/4), so one native token is the whole chain. |
| D4 | Peg-in is a send from a Litecoin wallet to an `ltcmweb1` address. Peg-out is a send from the MWEB wallet to a Litecoin address. Change of a peg-in stays on the Litecoin side. | What Litecoin Core does (`IsChangeOnMWEB` false); the user's Litecoin balance does not silently migrate into MWEB. |
| D5 | Swap, CrossPay, private send and WalletConnect never see the MWEB token. | Exchanges and providers do not accept MWEB deposits; a provider address is always a Litecoin address. |
| D6 | No watch account, no extended-key account, no address-only restore for MWEB. | Finding outputs needs the scan secret; an address alone cannot sync. |
| D7 | MWEB history is written on device when it happens and is never rebuilt from the chain. The restore screen and the wallet info say so. | Cut-through: the chain cannot replay spent outputs or sends (see Restore below). |
| D8 | New module `walletkit-chain-litecoin-mweb` depending on `walletkit-chain-bitcoin` (for `LitecoinKit`, the Litecoin adapter and the bitcoin send services). The peg-in hook the Litecoin side needs is an interface in `walletkit` core (`IPegInMwebAdapter`), resolved through `adapterManager` at send time. | One module per chain, as every chain since the modularisation; the bitcoin module must not depend on the MWEB kit. |

## Delta matrix

Rows reference [checklist.md](../checklist.md) items. "Same" means the
Litecoin behaviour is reused unchanged; every other row carries its reason.
`file:line` points at the Litecoin code the row changes or copies.

### ADDR · Addresses and input

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| A1 | Address formats | Base58 `L`/`M`/`3` and bech32 `ltc1` via `AddressHandlerBase58`, `AddressHandlerBech32` (`SegwitAddressConverter`) | New `AddressHandlerMweb`: bech32 with HRP `ltcmweb`, version byte 0, 66-byte payload (two compressed keys), checksum checked, HRP `tmweb` rejected on mainnet. Uses the kit's `MwebAddress.decode`. | Standard bech32 caps at 90 characters; MWEB addresses are 117. The kit's codec is tested against ltcd vectors. | ADDR-1 | `walletkit-chain-bitcoin/.../modules/address/BitcoinAddressHandlers.kt` |
| A2 | Address → chain and token | `BitcoinAddress.derivation` → `TokenType.Derived`; null falls back to BIP84 | The MWEB handler returns a plain `Address(hex, blockchainType = LitecoinMweb)`; the token is the chain's single native token. The Litecoin plugin also registers the MWEB handler (for peg-in, A5), returning an `Address` with `blockchainType = Litecoin` and no derivation. `BitcoinAddress.tokenType` is not involved. | A parsed `ltcmweb1` must never reach a derivation wallet's kit as a Litecoin address; keeping it out of `BitcoinAddress` makes that structural. | ADDR-3 | `walletkit/.../entities/Address.kt` |
| A3 | Normalization | Converters re-encode; stored value is the canonical form | The handler stores the re-encoded lowercase bech32 string from the decoded keys. | bech32 is case-insensitive; `LTCMWEB1…` and `ltcmweb1…` must be one contact and one address. | ADDR-2 | same as A1 |
| A4 | Send validation, MWEB wallet | `BitcoinAddressValidator` → `adapter.validate` | New validator for the MWEB token: accepts an `ltcmweb1` address (MWEB→MWEB) and any Litecoin address (peg-out) via `LitecoinMwebKit.validateDestination`. | Peg-out is a normal send in the MWEB wallet. | ADDR-1 | `walletkit-chain-bitcoin/.../send/address/BitcoinAddressValidator.kt` |
| A5 | Send validation, Litecoin wallet | Litecoin kit rejects anything it cannot pay | Accepts an `ltcmweb1` destination when the account has a Litecoin MWEB wallet enabled and synced (`adapterManager.getAdapterForToken<IPegInMwebAdapter>`); otherwise an error "Enable Litecoin MWEB to send to MWEB addresses". | The peg-in is built and broadcast by the daemon, which only runs for an enabled MWEB wallet. Starting it on demand for a one-off send is a later option. | ADDR-3 | same as A4 |
| A6 | Peg-in to another wallet's MWEB address | n/a | Supported: the Litecoin wallet pays any `ltcmweb1` address through `LitecoinMwebKit.pegIn(litecoinKit, address, value, feeRate)`; a foreign destination gets no own output, so the row confirms from the Litecoin txid. `mwebd` `Create` builds a peg-in with no MWEB inputs for any recipient. | Paying a friend privately is the main reason to send to an MWEB address. | ADDR-3, TX-1 | kit `LitecoinMwebKit.kt` `pegIn`, `MwebPegIn.kt` |
| A7 | Account types | Mnemonic, `HdExtendedKey` (xprv/xpub), `BitcoinAddress` watch | Mnemonic only. `BlockchainType.supports(accountType)` returns true only for `Mnemonic`; `WatchAddressViewModel` maps the chain to `Unsupported`. | D6: the scan secret comes from the BIP39 seed; there is no xpub-style public scanning. | ADDR-3, ACC-1 | `walletkit/.../core/MarketKitExtensions.kt:420`; `walletkit/.../watchaddress/WatchAddressViewModel.kt:247` |
| A8 | Deep links, QR, main-screen scanner | `litecoin:<addr>?amount=` → Litecoin token by derivation; the scanner picks the chain by the first handler that accepts the address | Both chains use `uriScheme = "litecoin"` and both accept `ltcmweb1` (A2), so the scanner would see two chains claim one address. Rule: an `ltcmweb1` address resolves to the Litecoin MWEB wallet when the active account has one enabled, otherwise to a Litecoin wallet as a peg-in. Within a wallet's own send screen only that wallet's handlers run, so no ambiguity there. | One URI scheme for both sides, as Litecoin Core and Cake emit; the scanner needs a tie-break. | ADDR-1 | `walletkit/.../core/factories/AddressParserFactory.kt:6`; `walletkit/.../modules/address/AddressParserChain.kt` |
| A9 | Memo | OP_RETURN up to 80 bytes | None for the MWEB token and none for a peg-in. `sendMemoSupport` returns null when the destination is an MWEB address. | The daemon turns every non-MWEB output of a peg-in into a peg-out; an OP_RETURN output would corrupt the transaction. MWEB outputs have no memo field the app exposes. | ADDR-4, TX-7 | `BitcoinFamilyPlugin.sendMemoSupport` |

### AMT · Amounts and decimals

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| M1 | Units | Satoshi `Long` in the kit, `BigDecimal` with 8 decimals in the app (`satoshiToBTC`) | Same: litoshi `Long` end to end, `BigDecimal` at the adapter boundary. | — | AMT-1 | `BitcoinBaseAdapter.satoshiToBTC` |
| M2 | Minimum amount | Litecoin dust (546 litoshi) from the kit | MWEB→MWEB: 1 litoshi (no dust rule inside MWEB). Peg-out: the Litecoin dust limit applies to the peg-out output; the adapter enforces it before the daemon does. Peg-in: same dust limit on the MWEB output value is not required, but the kit rejects ≤ 0. | Different sides, different rules; the user must get the error before signing. | AMT-2, TX-3 | `BitcoinBaseAdapter.minimumSendAmount` |
| M3 | Fee | sat/vB × virtual size | MWEB fee = weight × 100 litoshi per weight unit scaled by the chosen fee rate (`MwebFee.estimate`, mirrors `mweb.EstimateFee`: kernel 3, output 18, peg-out 42 bytes per weight). Peg-in fee = Litecoin fee (inputs + peg-in output + 41 vbytes for the next block's HogEx input) + MWEB fee. Confirmation shows one total with the breakdown. | Two fee domains in one transaction; the user sees one number. | TX-1, TX-3 | kit `MwebFee.kt`; `SendBitcoinFeeService.kt` |
| M4 | Available / max | `availableBalance(feeRate)` independent of destination | `maximumSpendableValue(address, feeRate)` depends on the destination: a peg-out output weighs more than an MWEB output. The amount field re-evaluates "max" when the address changes. | Otherwise "max" to a Litecoin address fails on insufficient balance. | TX-3 | `SendBitcoinAmountService.kt` |
| M5 | Unconfirmed balance | Unconfirmed incoming counted in `available` by the kit | Confirmed outputs → `BalanceData.available`; mempool outputs (including own change) → `BalanceData.pending`; kit's `MwebBalance.spendable/unconfirmed`. | The daemon does not spend unconfirmed MWEB outputs; showing them as available would make sends fail. | AMT-2 | `BitcoinBaseAdapter.balanceData` |
| M6 | Peg-out maturity | Hodler timelock → `BalanceData.timeLocked` | HogEx outputs (peg-outs) → `BalanceData.timeLocked` for 6 confirmations through the kit's `HogExOutputMaturityRule`; the locked-balance sheet gets copy "spendable after 6 confirmations". | Litecoin consensus; before the rule the kit would try to spend them and the peer would reject. | AMT-2, TX-3 | kit `HogExOutputMaturityRule.kt`; `BitcoinBaseAdapter.balanceData` |
| M7 | Fiat value of fee | Shown | Same for all fees; no limits or ceilings exist. | — | AMT-3 | — |

### HIST · Transaction history

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| H1 | Source of history | Replayed from the chain on every sync; rescan rebuilds it | Kit's Room table, written when the event happens; a rescan never deletes rows. Restore from seed yields only unspent incoming outputs. | Cut-through (D7). | HIST-1 | kit `storage/MwebDatabase.kt` |
| H2 | Record types | `BitcoinIncomingTransactionRecord`, `BitcoinOutgoingTransactionRecord` | New `MwebTransactionRecord` with kinds Incoming, Outgoing, PegIn, PegOut, SpentElsewhere; `mainValue` = amount, fee when known. | MWEB→MWEB has no Litecoin txid, no inputs/outputs the Bitcoin record expects, no lock info, no RBF. | HIST-1 | `walletkit/.../transactionrecords/bitcoin/` |
| H3 | Peg-in rows | n/a | Litecoin wallet: outgoing to `ltcmweb1…` ("To MWEB" label; the Litecoin record recognises the witness v9 peg-in output). MWEB wallet: incoming "From Litecoin". Two rows in "All", one per chain. | Same as a self-send between two own wallets today: two wallets, two rows. | HIST-1 | `BitcoinBaseAdapter.transactionRecord` |
| H4 | Peg-out rows | n/a | MWEB wallet: outgoing to the Litecoin address. Litecoin wallet: incoming row appears when the next block's HogEx confirms (never pending), amount time-locked per M6. | Peg-outs never enter the mempool as a Litecoin transaction. | HIST-3 | — |
| H5 | Timestamps after restore | Exact block time | Recovered outputs carry the height the daemon serves; for outputs older than the last 4000 blocks it is the MWEB header stride (±100 blocks). The block time exists only for heights the kit holds headers for, i.e. above its checkpoint; older outputs have no date and show "Unknown date" in the list header and the details, sorted by height among themselves. | Light client serves headers every 100 blocks except the recent window; the checkpoint bundle holds no headers below its start, and estimating from the 150 s block interval drifts about a day per year. | HIST-3 | kit `MwebTransactionInfo.timestamp`; `MwebTransactionRecord.blockTimestamp` |
| H6 | Explorer link | blockchair transaction URL | Peg-in and peg-out: blockchair URL of the Litecoin txid. MWEB→MWEB: no link (`getTransactionUrl` unused; the info page hides the row). | The transaction has no public id; only a kernel exists. | HIST-1 | `LitecoinAdapter.getTransactionUrl` |
| H7 | Spent elsewhere | n/a | An output that leaves the unspent set with no local send is a row "Spent in another wallet", amount = output value, no recipient. | Another wallet on the same seed (Cake, Core) can spend our outputs; silence would make the balance drop unexplained. | HIST-3 | kit `MwebTransactionType.SpentElsewhere` |
| H8 | Dust and poisoning | Suspicious-transaction filter hides dust | Same filter on incoming MWEB amounts. Lookalike senders do not exist: there is no visible sender. | — | HIST-6 | — |
| H9 | Tokens, approves, fee-only, mirror events | — | n/a: MWEB has no tokens, contracts or approves. | — | HIST-2, HIST-4, HIST-5, HIST-7 | — |

### TX · Building, signing, sending

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| T1 | Bytes confirmed = bytes signed | `rawTransaction()` previews, `send()` rebuilds with the same inputs | MWEB→MWEB and peg-out: the daemon builds and signs in one `Create`; the fee shown comes from the kit's estimator with the same inputs, and the kit asserts the daemon's fee equals it before broadcast. Peg-in: the kit asserts the peg-in output value equals amount + MWEB fee and that the daemon returned exactly one output before the Litecoin kit signs. | The daemon, not the app, serializes the final transaction. | TX-1 | kit `LitecoinMwebKit.send`, `pegIn` |
| T2 | Chain id | None in a Litecoin transaction | Same; the MWEB kernel commits to no chain id either. Testnet addresses are rejected by HRP (A1). | — | TX-2 | — |
| T3 | Balance check | Kit selects confirmed and unconfirmed-own outputs | Only confirmed MWEB outputs; fee rounded up by the estimator; peg-out checks amount ≥ Litecoin dust (M2). | — | TX-3 | kit `selectCoins` |
| T4 | Pending record vs broadcast | bitcoin-kit stores the transaction, then broadcasts; a lost response leaves it stored and re-sent | Kit inserts the Pending row before `Broadcast`. An explicit daemon rejection removes it and throws `BroadcastRejected`; a transport failure keeps it Pending and throws `BroadcastUnknown`, which the app shows as "status unknown", never as failed. The Utxos stream confirms it later. | A lost response must not read as "failed" and must not let the user pay twice. | TX-4, TX-5 | kit `LitecoinMwebKit.send`, `pegIn` |
| T5 | Retry | Resends the same stored bytes | A retry rebuilds; the new transaction spends the same coins, so at most one confirms. Acceptable because MWEB coins reserved by a pending send are excluded from the next selection. | — | TX-4 | kit `MwebBalance` |
| T6 | Sequence / nonce | n/a (UTXO) | n/a; two sends are serialized by the kit's mutex and pending coins are excluded. | — | TX-6 | kit `dbMutex` |
| T7 | Range checks | Kit | Address version must be 0 and payload exactly 66 bytes; amounts are positive `Long` litoshi below the 84 M LTC supply. | — | TX-7 | kit `MwebAddress.decode` |
| T8 | Speed up / cancel (RBF) | `ResendBitcoinPage` rebuilds the transaction | Not offered for MWEB records, and disabled on the Litecoin-side peg-in row. | The replacement builder drops the MWEB extension; the replacement would be invalid or, worse, pay the peg-in output without a kernel. | TX-1 | `BitcoinFamilyPlugin.resendTransactionPage`; `RbfSupport.kt` |
| T9 | Coin control, UTXO expert mode, timelock plugin | Offered | Not offered for the MWEB token in v1 (`sendSettingsPage` returns null). Peg-in from a Litecoin wallet keeps Litecoin coin control. | Kit selects MWEB coins; exposing `unspentOutputs()` is later work. | — | `BitcoinFamilyPlugin.sendSettingsPage` |
| T10 | Broadcast path | Litecoin peers via bitcoin-kit | Peg-in and every MWEB transaction go through the daemon's `Broadcast`; the Litecoin kit only registers the signed peg-in as pending (`registerPendingTransaction`). | A plain Litecoin broadcast strips the MWEB extension and the transaction is invalid. | TX-1 | kit `pegIn` |

### NET · Nodes and APIs

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| N1 | Network source | SPV peers (bloom) chosen by bitcoin-kit; API restore mode optional | Daemon connects to Litecoin peers advertising `NODE_MWEB_LIGHT_CLIENT`; no API mode, no restore-mode choice. The Litecoin blockchain-settings row keeps its sync-mode setting for the four derivations only. | MWEB data exists only on the P2P network. | NET-3 | `BitcoinFamilyPlugin.blockchainSettingsItem` |
| N2 | Zero balance | Kit state Synced after headers + bloom | Balance is trusted only once `mwebUtxosHeight ≥ blockHeaderHeight` and the header tip is within tolerance (kit `MwebSyncState.Synced`); before that the adapter reports Syncing with progress and stage. | An empty unspent set during sync is not a zero balance. | NET-1 | kit `pollStatus` |
| N3 | Checkpoint bundle | bitcoin-kit ships checkpoints in code | Kit ships `checkpoint-mainnet.bin` / `checkpoint-testnet.bin` in assets; refreshed at release time, block hash checked against an explorer (kit README). Existing data dirs ignore the bundle. | A wrong bundle sends every client down a wrong chain. Release checklist item. | NET-3 | kit `src/main/assets/mweb/` |
| N4 | Tor | JVM socket proxy properties set by `TorManager` cover bitcoin-kit | The Go daemon ignores JVM properties: the adapter passes `socks5://127.0.0.1:<port>` as `proxyUrl` when Tor is on and restarts the daemon when Tor connects or is turned off, as the Monero and Zano adapters do. | Otherwise MWEB traffic bypasses Tor while the app says Tor is on. | NET-5 | `walletkit-chain-monero/.../MoneroAdapter.kt:141`; `walletkit-chain-zano/.../ZanoKitManager.kt:48` |
| N5 | Stalls and restarts | bitcoin-kit reconnects | `onSyncStalled` → `kit.refresh()` (daemon restart); the daemon's own peer backoff after a dropped leafset request is in the kit (patch 0002). Network-change restart as the Monero adapter does. | Testnet has two reachable peers; a stall is permanent without a restart. | NET-3 | `ZcashChainPlugin.onSyncStalled` as pattern |
| N6 | Paging | n/a | `Utxos` is a server stream; the kit persists `syncedHeight` and resumes from it minus a rescan margin. No open-ended loop. | — | NET-2 | kit `consumeUtxos` |
| N7 | Error states | Kit state NotSynced(error) | Same: daemon start failure, gRPC unavailable and sync errors are distinct `NotSynced` causes; "no outputs" is never an error. | — | NET-4 | kit `MwebSyncState` |
| N8 | Custom nodes | None for bitcoin family | None; `.onion` peers follow Tor automatically. | — | NET-5 | — |
| N9 | Storage footprint | ~50 MB per Litecoin wallet | ~100 MB per device shared by all MWEB wallets after the checkpoint (hand-over measurement), up to 700 MB if the fallback genesis sync runs. The manage-wallets info sheet states the size once. No opt-in gate. | Spike recommendation (c), reduced to a notice after D2 landed. | — | `ManageWalletsService.hasInfo` |
| N10 | Background | Kits stop in background via `BackgroundManager` | Same hooks (`onEnterBackground/Foreground`); the daemon keeps running while any MWEB wallet is active in foreground. | — | — | `BitcoinBaseAdapter` background handling |

### ACC · Accounts and app-wide surfaces

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| C1 | Watch accounts | Litecoin address watch | No MWEB wallet on a watch or extended-key account (A7), so no signing path is reachable. | D6 | ACC-1 | `Token.supports` |
| C2 | Local backup | Enabled wallets saved by `tokenQueryId` | The id is `litecoin-mweb\|native`; it round-trips once market-kit knows the chain. Older app versions map the uid to `BlockchainType.Unsupported` and skip the wallet. MWEB history is **not** in the backup in v1 (tech debt, see Restore). iOS must use the same uid (PAR). | — | ACC-2 | `walletkit/.../backuplocal/fullbackup/BackupProvider.kt:170` |
| C3 | Delete account | `LitecoinAdapter.clear(accountId)` | `LitecoinMwebChainPlugin.clearAccountData` → `LitecoinMwebKit.clear(context, network, accountId)` deletes the per-wallet database; the shared chain data dir stays (it holds no account data) and is removed only when the last MWEB wallet on the device is gone, or never. | — | ACC-3 | `ZanoChainPlugin.clearAccountData` as pattern |
| C4 | Swap and CrossPay | USwap maps provider assets to chains it enumerates explicitly | The new chain is not in any provider's chain list, so it is excluded without code; the MWEB plugin leaves `swapDestinationAddress`, `swapSourceAddresses` and `sendTransactionService` for swap at their null defaults. | D5 | ACC-4 | `walletkit/.../multiswap/providers/USwapProvider.kt:168` |
| C5 | WalletConnect | n/a | n/a | — | ACC-5 | — |
| C6 | Market-kit chain | `BlockchainType.Litecoin`, uid `litecoin` | New `BlockchainType.LitecoinMweb`, uid `litecoin-mweb`, in `uid`, `fromUid`, `toString`; the market API publishes the blockchain (name "Litecoin MWEB") and one token row `{coin_uid: litecoin, blockchain_uid: litecoin-mweb, type: native}`; the bundled `initial_coins_list` gets the same. No `TokenType` change. | Closed sealed class in market-kit; both platforms serialize the uid into backups (C2). iOS adds the same enum case and uid. | PAR-2 | market-kit-android `models/BlockchainType.kt`; `marketkit/src/main/assets/initial_coins_list` |
| C7 | Chain constants in the app | Per-chain exhaustive `when`s | New branches: `title` "Litecoin MWEB", `order` next to Litecoin, `tokenIconPlaceholder`, `brandColor` null, `feePriceScale`, `chainId` null, `isEvm` false, `restoreSettingTypes` empty, `supports` (A7), `supported` list, `uriScheme`/`removeScheme` (A8), block time 150 s, `isSyncWithProgress` true with default progress 10, `hasInfo` true for the restore/size note, `TransactionRecordRepository` merge rule untouched. `Blockchain.description` reads "LTC". | The compiler points at each `when` when the sealed class grows; the Zano chain is the template. | — | `MarketKitExtensions.kt`; `AddressParserFactory.kt`; `Extensions.kt:211`; `BalanceViewItemFactory.kt:138`; `ManageWalletsService.kt:148`; `ConfiguredTokenInfoViewModel.kt:57` |
| C8 | Adapter creation | `BitcoinFamilyPlugin.createAdapter` by `TokenType.Derived` | New `LitecoinMwebChainPlugin.createAdapter` for `TokenType.Native` → `LitecoinMwebAdapter(wallet, backgroundManager, torManager)`; registered in `MainApp` with the other plugins. | — | — | `app/src/main/java/io/horizontalsystems/bankwallet/MainApp.kt:42` |
| C9 | Receive | Address format label "Native SegWit (BIP84)"; rotates to the first unused address | No address-format label (native token); the network name identifies it. `receiveAddress()` is the next unused index ≥ 1 (index 0 is change). Used addresses listed without explorer links. | Daemon convention; an explorer cannot show MWEB addresses. | — | `ReceiveAddressViewModel.kt:86` |
| C10 | Parity fixture | One block per chain | `chain-behavior-parity.txt` gains a `chain litecoin-mweb` block (`nativeTokenQueries=litecoin-mweb\|native`, `supports=Mnemonic`, `restoreSettingTypes=` empty) and the uid in `supported=`. The Litecoin block is unchanged. | — | — | `app/src/test/resources/chain-behavior-parity.txt:1` |
| C11 | Private keys screen | BIP32 root, account xprv | No extra rows; the MWEB scan/spend secrets are derived from the same seed and are not shown. | Showing a raw scan key invites pasting it into untrusted tools; nothing in the ecosystem imports it. | — | `BitcoinChainPlugin.privateKeyRows` |

### PAR · Parity with iOS

| # | Area | Analog does | New chain does | Why | Checklist | file:line |
|---|---|---|---|---|---|---|
| P1 | Address vectors | Same seed → same 20 addresses per derivation | Same seed → same MWEB addresses for indices 0..20 on both platforms; vectors come from the kit's unit tests (derived with ltcd, not with the kit's own code) and one Cake Wallet cross-check. | — | PAR-1 | kit `MwebKeychainTest.kt` |
| P2 | Backup and test vectors | Compared | Token id (C6) and the `test-vectors.md` watch cases compared; the MWEB history-not-in-backup limitation stated on both platforms. | — | PAR-2 | — |
| P3 | Findings | Checked on the other platform | Same; the T4 and A6 rules apply to any iOS port of the kit logic. | — | PAR-3 | — |

## Restore behaviour (differs from every other coin we ship)

- Restoring from seed recovers the **exact balance** and the unspent
  incoming outputs, dated only when mined above the kit's header checkpoint
  (H5). Spent incomings and sends are gone (H1).
- Consequences for the app: the MWEB wallet info sheet and the restore flow
  say "Transaction history before this restore is not recoverable; the
  balance is". A rescan clears chain data only, never the history table.
- Not in v1, recorded as tech debt: carrying the MWEB history table in the
  encrypted backup file so a restore from our own backup keeps the history.

## Where the code goes

market-kit-android (Phase 2, own branch, pinned by commit like the kits):

- `models/BlockchainType.kt`: `LitecoinMweb` object, uid `litecoin-mweb`.
- `assets/initial_coins_list`: blockchain and native token entries.
- Market API: the same blockchain and token rows (backend change).

New module `walletkit-chain-litecoin-mweb` (D8), modelled on
`walletkit-chain-zano`:

- `chain/litecoinmweb/LitecoinMwebChainPlugin.kt`: `createAdapter` (C8),
  `addressHandlers` (A1), `addressValidator` (A4), `clearAccountData` (C3),
  `sendTransactionService` for plain sends, `sendTransactionData`,
  `sendMemoSupport` null (A9), `onSyncStalled` (N5), `statusInfo`; swap,
  resend and send-settings hooks stay at their null defaults (C4, T8, T9).
- `core/adapters/LitecoinMwebAdapter.kt`: `IAdapter`, `IBalanceAdapter`,
  `IReceiveAdapter`, `ITransactionsAdapter`, `ISendMwebAdapter` (validate,
  estimate, send) and `IPegInMwebAdapter` (estimatePegInFee, pegIn). Wraps
  `LitecoinMwebKit`; Tor proxy handling as the Monero adapter (N4).
- `modules/send/mweb/SendTransactionServiceMweb.kt`: MWEB→MWEB and peg-out.

`walletkit-chain-bitcoin` (peg-in side; it depends on the MWEB kit for the
address codec, which the root JitPack artifact of bitcoin-kit pulls in anyway):

- `modules/address/BitcoinAddressHandlers.kt`: `AddressHandlerMweb`, used by
  both chains; `LitecoinChainPlugin.addressHandlers` registers it (A2).
- `core/IPegInMwebAdapter.kt`: the peg-in hook (it names `LitecoinKit`, so it
  cannot live in `walletkit` core).
- `BitcoinAddressValidator` accepts `ltcmweb1` when an `IPegInMwebAdapter`
  exists for the account (A5).
- `SendTransactionServiceBtc` gets a peg-in branch: fee from
  `estimatePegInFee`, send through `pegIn` (M3, T10); `RbfSupport` and
  `resendTransactionPage` refuse the peg-in row (T8).
- `BitcoinBaseAdapter.transactionRecord` labels the v9 peg-in output (H3).

`walletkit` core: `ISendMwebAdapter` in `Interfaces.kt`,
`MwebTransactionRecord` (H2), chain constants (C7), `SendTransactionData.Mweb`
and `SendTransactionResult.Mweb`, strings in nine locales, parity fixture
(C10). The scanner tie-break (A8) is not implemented yet: today the Litecoin
handler, registered first, claims an `ltcmweb1` address scanned from the main
screen, which opens a peg-in from a Litecoin wallet.

`app`: register `LitecoinMwebChainPlugin` in `MainApp`;
`gradle/libs.versions.toml`: `bitcoinKit` and `marketKit` → the branch
commits; new `kit-litecoin-mweb` library entry.

## Open questions

1. Confirm the uid `litecoin-mweb` and the title "Litecoin MWEB" with iOS
   (C6); the uid ends up in backups on both platforms.
2. Funded verification: no testnet LTC was available to the kit session
   (faucets need captchas). Peg-in, MWEB→MWEB, peg-out, `Spent`
   reconciliation and restore with funds are implemented but untested end to
   end. Either obtain testnet4 LTC or verify on mainnet with a small amount
   during Phase 3.
3. Whether to start the daemon on demand for a peg-in from an account without
   an MWEB wallet (A5 alternative).
