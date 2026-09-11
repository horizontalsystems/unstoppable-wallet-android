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

- [ ] Minimum / maximum amount: Bitcoin dust, Stellar per-destination minimum. Currently
      only surfaced as confirmation cautions.
- [ ] Bitcoin fee-inclusive available balance and the "leave coin for fee" warning at max.
      Sending the full BTC balance fails on confirmation. (EVM service adjusts automatically.)
- [ ] Bitcoin fee-rate warnings (low fee, risk of getting stuck) on the send page.
- [ ] Inline caution under the amount field. Errors show only in the button title.

## Per-chain features

- [x] Tron: activation fee and bandwidth/energy rows on confirmation.
- [ ] Tron: max native amount reduced by fee, zero-TRX rule.
- [ ] TON fee estimation on the send page (Next was gated on it).
- [ ] Zcash send-to-self rejection: verify the entry-screen validator covers it.
- [ ] Bitcoin UTxOs row shows total/total in auto mode (auto-selected count is only known
      after the fee estimate).

## Confirmation

- [ ] Bitcoin rows: UTXO count, timelock, RBF. Every service returns an empty field list.
- [ ] Pre-proceed no-internet check (old EVM, Solana, Tron screens showed a HUD).

## Cosmetic

- [ ] Coin/fiat primary input mode is no longer persisted (both fields shown at once).
- [ ] Amount field: shake on invalid input, clear button, MAX button (percent bar covers max).
- [ ] Memo max lengths copied from old screens (120/250), not from chain specs.

## Cleanup

- [x] Delete per-chain send screens and view models, `EnterAddressPage`, `SendPage`,
      `ChainPlugin.SendScreen` / `ChainSendScreenArgs`.
