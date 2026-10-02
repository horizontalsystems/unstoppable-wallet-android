# Chain integration checklist

Each item names a rule and, where we have one, the defect that taught it.
Reference items by ID in the delta matrix and in review findings. An item that
does not apply to a chain gets a row saying why, not silence.

## ADDR · Addresses and input

- **ADDR-1** Address validation checks everything the format carries:
  checksum, version byte or HRP, length. Validation is never optional on any
  entry path: send, contacts, watch account, deeplink, QR, backup restore.
- **ADDR-2** Addresses are normalized before they are stored or compared, so
  one address cannot become two accounts or two contacts.
  *THOR/Maya watch addresses are stored as typed, so `THOR1…` and `thor1…`
  are two accounts.*
- **ADDR-3** Every format the chain accepts on input is either supported or
  rejected with a message. An address in an alternate format must never
  restore as a working-looking dead account.
  *A hex Tron address in a backup restores as an account that never syncs.*
- **ADDR-4** If the chain requires a memo, tag, or destination flag for some
  recipients, send checks it before signing.

## AMT · Amounts and decimals

- **AMT-1** Amounts are integers or `BigDecimal` from end to end. No `Double`,
  no locale-dependent parsing.
- **AMT-2** Every place that rescales decimals keeps special values special.
  Check the maximum (`2^256−1`, the chain's trust-line default) before
  rescaling, and decide what the user sees for it.
  *Arc rescales `0x3600` amounts by 6 decimals but checks `isMaxValue` against
  18, so an unlimited approve shows as a huge USDC amount with a fiat value.*
- **AMT-3** A limit, allowance, or ceiling is never shown with a fiat value.
- **AMT-4** Token decimals and precision come from the chain or MarketKit and
  are checked against the chain's own limits (XRPL issued amounts keep 16
  significant digits).

## HIST · Transaction history

- **HIST-1** Each transaction appears once in each wallet and in "All", under
  every filter, even when it matches several history tags.
- **HIST-2** Mirror events and native-token interfaces are folded into the
  native movement, never shown as a second row or a separate token.
- **HIST-3** The amount shown is the amount that moved, not the amount
  requested. *XRP falls back to `Amount` when `delivered_amount` is missing;
  partial payments then show more than was received.*
- **HIST-4** Fee-only transactions appear in "All" and not in the coin wallet
  as a transfer.
- **HIST-5** Approves are shown as approves, never as an incoming amount.
- **HIST-6** Spam and poisoning: dust below the threshold is hidden; a token
  with a real ticker from another contract or issuer never mixes with the
  real one; a lookalike sender is not shown as a normal incoming.
- **HIST-7** A token's identity includes its contract or issuer. Where two
  tokens can share a code, the issuer is shown.

## TX · Building, signing, sending

- **TX-1** The bytes the user confirmed are the bytes that are signed and
  broadcast. The fee shown is the fee signed.
- **TX-2** The chain ID or network identifier is inside the signed payload.
- **TX-3** The send checks `balance ≥ amount + fee + reserve` with the
  chain's reserve rules and rounds the fee up, never down.
- **TX-4** A lost or ambiguous submit response is "unknown", not "failed".
  The pending record is written before submit, and a retry resends the same
  signed bytes rather than building a new transaction.
  *XRP writes pending only after submit returns; a timeout reads as failure,
  and sending again can pay twice.*
- **TX-5** A pending transaction is marked failed only on an explicit
  "not found after expiry" from the chain, never because a lookup threw.
- **TX-6** Sequence or nonce use is serialized per account, so two sends in
  flight cannot share one.
- **TX-7** Every encoded field is range-checked; out-of-range values are
  rejected, never truncated. Memo and payload size limits are enforced.

## NET · Nodes and APIs

- **NET-1** An empty, missing, or "account not found" response is not a zero
  balance unless the chain says so from a node that is in sync.
- **NET-2** Every paging loop has a cap. A node that keeps returning a marker
  cannot hang sync.
- **NET-3** Node errors that mean "this node is behind or broken" fail over or
  surface as a sync error; they do not block sync and send for good.
- **NET-4** Network error, parse error, and "no data" stay distinct states.
  `runCatching { }.getOrNull()` that turns all three into "nothing" needs a
  reason in a comment.
- **NET-5** Custom node URLs follow the app rule: HTTPS, except `.onion`.

## ACC · Accounts and app-wide surfaces

A new chain plugs into flows that are not in its module. Check each one.

- **ACC-1** Watch accounts cannot reach any signing path: send, swap,
  activation, speed-up and cancel, deeplinks, donate, WalletConnect.
- **ACC-2** Local backup: export and restore round-trip every account type
  the chain adds, private keys at their fixed byte length. One unreadable
  account must not abort the whole restore. The iOS type names for the same
  account are accepted.
- **ACC-3** Deleting an account removes everything the chain stored for it
  (`ChainPlugin.clearAccountData`) and leaves no state that the next sync
  binds to another account. *WalletConnect: after an account is deleted, its
  live sessions are rebound to the active account.*
- **ACC-4** Swap and CrossPay: the deposit address and memo the provider
  returns are the ones signed, and the user sees them separately from the
  final recipient.
- **ACC-5** WalletConnect, if the chain supports it: every operation type in
  a request is shown with its parameters. Unknown operations get a warning
  that is not hidden by a known one in the same request.

## PAR · Parity with iOS

- **PAR-1** The same seed gives the same first 20 addresses for every
  derivation the chain supports on both platforms.
- **PAR-2** Backup files and test-vector results are compared across
  platforms.
- **PAR-3** A finding on one platform is checked on the other before it is
  closed.
