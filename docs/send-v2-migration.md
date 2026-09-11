# Unified send migration

Tracks what the unified send flow (`walletkit/.../modules/send/v2/`) still lacks compared
with the per-chain send screens it replaces, and what is left to remove. Tick items off as
they land.

## Done

- [x] One send page for every chain: tabs, amount with fiat, address entry, memo per chain,
      balance, percent bar, risky-address sheet.
- [x] One confirmation page on the chain send-transaction service (fee, fee/nonce settings,
      cautions, send).
- [x] Chain settings hook: Bitcoin (UTxOs, Inputs/Outputs, Time Lock, RBF) and Monero
      (UTxOs, with stale selections trimmed after a rescan).
- [x] Balance narrowed to manually selected Bitcoin and Monero outputs, and the
      confirmation validates against the selection.
- [x] Private tab on the existing private send confirmation.
- [x] All entry points open the new page: balance header, token page, context menu, deep
      links, donate. Prefill (address, amount, memo, hidden destination) and custom title.

## Send-page validation

- [x] Minimum amount: Bitcoin dust and Stellar's create-account minimum are checked on
      confirmation, with a title and description caution.
- [x] Sending the whole balance of a coin that pays its own fee: the confirmation reduces
      the amount by the fee for every chain (Bitcoin via its input selection, Solana net of
      the rent reserve). Zcash and Monero cannot estimate a fee for the full balance and
      still end in an insufficient-balance caution.
- [x] Bitcoin fee-rate cautions moved to the confirmation with the fee rate itself; the send
      page never sets a rate.
- [x] Inline caution under the amount field: not needed, the button title carries the
      error as on the swap screen.

## Per-chain features

- [x] Tron: activation fee and bandwidth/energy rows on confirmation.
- [x] Tron: max native amount reduced by fee, zero-TRX rule.
- [x] TON fee estimation happens on confirmation, like every other chain; a failed estimate
      is retried and then shown as a caution.
- [x] Zcash send-to-self rejection: the address entry screen's Zcash validator refuses the
      wallet's own address, so it never reaches confirmation.
- [ ] Bitcoin UTxOs row shows total/total in auto mode (auto-selected count is only known
      after the fee estimate).

## Confirmation

- [x] Bitcoin rows: timelock when set, replace-by-fee when switched off. Defaults are not
      shown, and the UTXO count stays on the settings page.
- [x] Offline: the Next button reads "No Internet" and is disabled while the device has no
      network, for every chain.

## Cosmetic

- [ ] Coin/fiat primary input mode is no longer persisted (both fields shown at once).
- [ ] Amount field: shake on invalid input, clear button, MAX button (percent bar covers max).
- [ ] Memo max lengths copied from old screens (120/250), not from chain specs.

## Cleanup

- [x] Delete per-chain send screens and view models, `EnterAddressPage`, `SendPage`,
      `ChainPlugin.SendScreen` / `ChainSendScreenArgs`.
