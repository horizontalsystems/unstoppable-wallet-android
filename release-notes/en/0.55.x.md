## 🚀 Version 0.55.0 Update
_Release date: May 14, 2026_

### ✨ Improvements

- **Added support for the Trezor hardware wallet**
  All models are supported for `BTC`, `LTC`, `BCH`, `DOGE`, `ETH`, `BSC`, `Polygon`, `Arbitrum`, `Optimism`, `Base`, and `Stellar`.
  `Dash` is supported only on `Trezor One` and `Model T`.
  `Solana` is supported only on `Model T`, `Safe 3`, `Safe 5`, and `Safe 7`.

- **Added a new premium feature: Pseudo Calculator**
  The app can look and behave like a regular calculator.
  If the calculation result matches the PIN code, the wallet opens.

- **Improved the push notification filter for new transactions**
  Spam transactions are no longer shown in push notifications.

- **The app no longer adds unknown tokens automatically**
  If a transaction arrives with an unknown token, that asset is no longer added to the wallet automatically.
  This helps keep unwanted assets hidden.

- **Reworked the display logic for unpaid balances in PirateCash and Cosanta**

- **Reworked recovery behavior for non-English seed phrases**

### 🐛 Fixes

- **Fixed stability and performance issues across the app**
