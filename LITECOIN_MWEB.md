# Litecoin MWEB: task context

Working notes for issue
[#9622 "Add LMWEB Support"](https://github.com/horizontalsystems/unstoppable-wallet-android/issues/9622).
LMWEB is Litecoin MWEB, the MimbleWimble Extension Blocks privacy layer of
Litecoin. This file is the entry point for whoever continues the work: what
was decided, where the pieces live, what is verified and what is not. The
detailed design is in `docs/chain-integration/litecoin-mweb/design.md`, the
measurements in `docs/chain-integration/litecoin-mweb/spike.md`.

## Status (2026-11-02)

- Kit, market data and app integration are implemented and build. A debug
  build runs on a phone: the MWEB daemon syncs to the chain tip in about
  half a minute, the wallet shows a receive address, both send forms accept
  MWEB addresses, the MWEB wallet's send screen opens.
- Nothing with funds has been exercised yet: peg-in, MWEB-to-MWEB send,
  peg-out, spent-output reconciliation and restore with a balance are all
  untested end to end. This is the next step.
- The market API does not yet serve the `litecoin-mweb` blockchain and
  token, so the wallet cannot be enabled on a normal install. See
  "Backend dependency".
- No pull request is open. Branches are listed below.

## Decisions

1. **Engine**: the embedded `mwebd` daemon (Go, MIT, used by Cake Wallet,
   Stack Wallet, Litewallet and Electrum-LTC) over local gRPC. The earlier
   pure-Kotlin prototype branch in bitcoin-kit-android is not used: it never
   verified key derivation and had no range proofs, peg-in or MWEB send.
2. **Model**: MWEB is its own blockchain, uid `litecoin-mweb`, title
   "Litecoin MWEB", with a single native token for the coin `litecoin`
   (code LTC, 8 decimals). It is not a token type on Litecoin. MWEB has no
   token layer of its own.
3. **Peg-in** is a send from a Litecoin wallet to an `ltcmweb1…` address;
   **peg-out** is a send from the MWEB wallet to a plain Litecoin address.
   Swap offers regular LTC only. Change of a peg-in stays on the Litecoin
   side, as Litecoin Core does.
4. **Keys**: scan key `m/1000'/0'`, spend key `m/1000'/1'`, the scheme Cake
   Wallet uses, so a seed restores across wallets.
5. **First sync** starts from a header checkpoint bundle shipped in the kit
   instead of genesis. Without it the first sync downloads the whole
   Litecoin header chain: about 400 MB on the wire, 700 MB on disk and 30
   minutes on a phone. The bundle must be refreshed at each kit release.
6. **Restore** recovers the exact balance but only the unspent incoming
   outputs: MWEB cut-through removes spent outputs from what the network
   serves, so spent receives and all sends are gone. History is written on
   device when it happens and never rebuilt by a rescan.
7. **Dates after restore**: an output mined below the kit's header
   checkpoint has a height (accurate to about 100 blocks) but no block time,
   because the client holds no headers there. Such rows show "Unknown date"
   and sort by height. Estimating from the 150 s block interval was
   rejected: it drifts about a day per year of output age. Exact dates are
   possible with an on-demand lookup, see "Possible follow-ups".
8. **Module**: `walletkit-chain-litecoin-mweb`, depending on
   `walletkit-chain-bitcoin` for the Litecoin kit and the bitcoin send
   services. The bitcoin module reaches the MWEB side only through the
   `IPegInMwebAdapter` interface in `walletkit` core, resolved at send time.

## Where the pieces live

| Piece | Repository and branch | State |
|---|---|---|
| `litecoinmwebkit` module (Go layer, JNI shim, Kotlin API, checkpoint bundles) and small `bitcoincore` additions | horizontalsystems/bitcoin-kit-android, branch `litecoin-mweb` (10 commits on master) | Not merged to master on purpose. Rebase on master when it moves. The app pins the branch commit hash through JitPack. |
| `BlockchainType.LitecoinMweb` and the initial coin list rows | horizontalsystems/market-kit-android, branch `litecoin-mweb` (2 commits on master) | Same: unmerged, pinned by hash. |
| App integration | this repository, branch `litecoin-mweb` on top of `develop` | Pushed, no pull request. |

Pins live in `gradle/libs.versions.toml` (`bitcoinKit`, `marketKit`). JitPack
builds any pushed commit of those repositories, so a kit change is consumed by
pushing it and updating the hash; no release is needed during development.
The bitcoin-kit root artifact aggregates all its modules, so `litecoinmwebkit`
resolves at the same version automatically.

### In this repository

- `walletkit-chain-litecoin-mweb/`: `LitecoinMwebChainPlugin`,
  `LitecoinMwebAddressValidator`, `LitecoinMwebAdapter` (balance, receive,
  history, MWEB send, peg-in hook), `SendTransactionServiceMweb`.
- `walletkit-chain-bitcoin/`: `AddressHandlerMweb` (also registered on the
  Litecoin plugin so `ltcmweb1` parses everywhere), the peg-in branch of
  `SendTransactionServiceBtc`, the MWEB destination path in
  `BitcoinAddressValidator`, `IPegInMwebAdapter`, and the address handler
  tests under `src/test`.
- `walletkit/`: `LitecoinMweb` branches in every exhaustive `when` over
  `BlockchainType`; `SendTransactionData/Result/Settings.Mweb`;
  `entities/transactionrecords/mweb/MwebTransactionRecord.kt` and its
  rendering in `TransactionViewItemFactory`, `TransactionInfoService`,
  `TransactionInfoViewItemFactory`, `TransactionViewItemFactoryHelper`;
  strings `Mweb_*`, `Send_Error_EnableMwebWallet`, `Transactions_UnknownDate`
  in all locales.
- `app/`: plugin registration in `MainApp.kt`, `ChainBehaviorParityTest`
  registration and the regenerated `chain-behavior-parity.txt`.

### Kit API in one glance

`LitecoinMwebKit(context, words, passphrase, walletId, networkType, proxyUrl)`
with `syncStateFlow`, `balanceFlow`, `transactionsFlow`,
`lastBlockHeightFlow`, `receiveAddress(index)`, `validateDestination`,
`estimateFee`, `maximumSpendableValue`, `send`, `estimatePegInFee(litecoinKit,
value, feeRate)`, `pegIn(litecoinKit, address, value, feeRate)`, static
`clear(context, network, walletId)`. Errors are `MwebSendError` subclasses;
`BroadcastUnknown` means the daemon could not tell whether the transaction
went out. `MwebTransactionInfo.timestamp` is null when the block time is
unknown. The kit's Room database rejects main-thread queries, so the app
adapter caches balance and receive address from the flows and runs fee,
send and peg-in calls on an IO dispatcher. Full description:
`litecoinmwebkit/README.md` in the kit repository.

## Backend dependency

The market API must serve, for the HS market data the app syncs:

- blockchain `litecoin-mweb`, name "Litecoin MWEB";
- token: coin `litecoin`, blockchain `litecoin-mweb`, type `native`,
  decimals 8.

Until it does, the wallet can only be enabled on a device whose market
database has those rows inserted by hand, and the coin syncer deletes them
again whenever the server-side timestamps change. The market-kit branch
already carries the rows in its initial coin list so a fresh install of a
build pinned to it has them.

## Verified so far

- Unit tests: `AddressHandlerMwebTest` (address codec against the ltcd
  vector), `ChainBehaviorParityTest`, walletkit and bitcoin module suites;
  translation check across the nine locales.
- On a phone, unfunded: daemon start from the checkpoint bundle, sync to
  tip, receive address, MWEB wallet history screen, send screen (opens,
  validates `ltcmweb1` and Litecoin addresses, no memo field for MWEB
  destinations because a memo would force a peg-out), Litecoin wallet
  send form accepting an MWEB address with the peg-in fee path.
- Spike numbers (see `spike.md`): first sync from genesis 30 minutes and
  750 MB on a phone; with the checkpoint bundle about 30 seconds and under
  50 MB; idle traffic cut from roughly 340 MB/day to 35 MB/day by the
  blocks-only and single-peer leafset patches; restart after a short
  absence re-syncs in seconds.

## Remaining work

1. **Funded verification** on mainnet with a small amount or on testnet4
   (faucets need captchas). In order: peg-in from the Litecoin wallet,
   MWEB-to-MWEB send and receive between two wallets, peg-out, the six
   confirmation maturity of the peg-out output on the Litecoin side,
   restore from seed on a second device (balance recovered, history limited
   as described), and the undated row for an output older than the
   checkpoint. Record results in
   `docs/chain-integration/litecoin-mweb/test-vectors.md` from the template
   in `docs/chain-integration/test-vector-template.md`.
2. **Review**: the role-separated review from
   `docs/chain-integration/README.md` against `checklist.md`, citing item
   IDs as `design.md` does.
3. **Backend**: request the market API rows above.
4. **iOS hand-off**: agree on the uid `litecoin-mweb` and the title (both
   end up in backups), the key scheme, the peg-in change policy and the
   restore semantics, so both platforms recover the same wallet.
5. **Release mechanics**: refresh the checkpoint bundle with the kit's
   `mkcheckpoint` tool and verify the printed block hash against an
   explorer before committing; a wrong bundle makes every client follow a
   wrong chain. Then merge the two kit branches and move the pins to
   release tags.

## Known gaps

- Scanning a QR with an `ltcmweb1` address from the balance screen picks the
  Litecoin wallet first: both the Litecoin and the MWEB handlers accept the
  address and the Litecoin one is registered first. A tie-break is not
  implemented.
- MWEB-to-MWEB transactions have no Litecoin transaction hash; the record
  uses its uid as the hash, so the explorer link on such rows is useless.
  Only the kernel is visible on chain anyway.
- MWEB history is not part of the wallet backup file. Since the chain
  cannot replay it, a backup is the only way to carry spent history to a
  new device.
- A peg-in from an account that has no MWEB wallet enabled is refused with
  a hint to enable it; starting the daemon on demand for that case is not
  implemented.
- Pending MWEB rows are dated by the device clock until confirmed.

## Possible follow-ups

- **Exact dates for old outputs.** Every block's MWEB header carries the
  output count at that block, and peers serve the MWEB header of any block.
  Bundling the block hash at each stride height (about 300 KB), fetching
  the 100 block headers of an output's window with a normal headers request,
  then binary-searching their MWEB headers on the output count finds the
  exact block and its time, once per undated output, for well under a
  megabyte. About 300 lines in the Go layer plus the bundle format and
  generator. The app and the database already accept the result since the
  timestamp is nullable.
- A cheaper middle ground is a block time per stride height in the bundle
  (about 38 KB): dates within four hours instead of none.
- Exact ordering of outputs inside one 100-block window needs the leaf index
  exposed from the daemon's UTXO stream; today such rows tie on height.

## Development notes

- The kit builds with JDK 17 (newer JDKs are rejected by its Kotlin
  toolchain). The Go layer is built offline per ABI with the recipe in the
  kit README; the resulting shared libraries are committed. Patches to
  `mwebd` and the neutrino fork live under `litecoinmwebkit/patches/`.
- Dev builds on a device with a secure window flag cannot be screenshotted;
  the UI hierarchy dump still works for inspection.
- The MWEB data directory is shared per device and network under the app's
  files directory; `LitecoinMwebKit.clear` deletes a wallet's own database
  and leaves the shared header chain in place.
