# Crypto Wallet Review Guidelines

Consumed by automated code review (CodeRabbit) and by human reviewers.

## 1. Secret material

**Secret material** = mnemonic phrases, BIP-39 seeds, private keys,
extended private keys, signing keys, passphrases, PINs.

- MUST NOT appear in logs, analytics, crash reports, telemetry.
- MUST NOT be written to clipboard automatically or from background
  business logic.
- Clipboard copy is allowed only as an explicit user action from a
  sensitive-data screen, ideally with a confirmation step and routed
  through the app's centralized clipboard helper.
- MUST NOT be persisted in `SharedPreferences`, `DataStore`, or plain
  files.
- Acceptable storage only:
  - Android: Android Keystore (`KeyStore.getInstance("AndroidKeyStore")`).

## 2. Randomness

Cryptographic randomness MUST use:
- Android: `java.security.SecureRandom`.

`kotlin.random.Random` and `java.util.Random` are prohibited in any
code path deriving nonces, salts, IVs, or private keys.

## 3. Android architecture

- Cryptographic work on `Dispatchers.IO` or a dedicated executor,
  never `Dispatchers.Main`.
- `StateFlow` / `SharedFlow` exposed to UI must not carry secret
  material.
- Log calls in crypto modules must be guarded by `BuildConfig.DEBUG`.
