---
description: "Use when performing rebranding, renaming packages, updating brand references, replacing Unstoppable with Quantum Wallet, changing io.horizontalsystems to com.quantum.wallet, or updating company name from Horizontal Systems to Quantum Chain."
---

# Rebranding Guide — Unstoppable → Quantum Wallet

## Package Mapping

All internal packages must be renamed. Apply these substitutions to `package` declarations, `import` statements, directory paths, XML references, and build config `namespace`/`applicationId` values.

| Old Package | New Package |
|-------------|-------------|
| `io.horizontalsystems.bankwallet` | `com.quantum.wallet.bankwallet` |
| `io.horizontalsystems.core` | `com.quantum.wallet.core` |
| `io.horizontalsystems.chartview` | `com.quantum.wallet.chartview` |
| `io.horizontalsystems.icons` | `com.quantum.wallet.icons` |
| `io.horizontalsystems.subscriptions.core` | `com.quantum.wallet.subscriptions.core` |
| `io.horizontalsystems.subscriptions.dev` | `com.quantum.wallet.subscriptions.dev` |
| `io.horizontalsystems.subscriptions.fdroid` | `com.quantum.wallet.subscriptions.fdroid` |
| `io.horizontalsystems.subscriptions.googleplay` | `com.quantum.wallet.subscriptions.googleplay` |

### Directory Path Mapping

Source directories follow the package structure:

| Old Path | New Path |
|----------|----------|
| `io/horizontalsystems/bankwallet/` | `com/quantum/wallet/bankwallet/` |
| `io/horizontalsystems/core/` | `com/quantum/wallet/core/` |
| `io/horizontalsystems/chartview/` | `com/quantum/wallet/chartview/` |
| `io/horizontalsystems/icons/` | `com/quantum/wallet/icons/` |
| `io/horizontalsystems/subscriptions/core/` | `com/quantum/wallet/subscriptions/core/` |
| `io/horizontalsystems/subscriptions/dev/` | `com/quantum/wallet/subscriptions/dev/` |
| `io/horizontalsystems/subscriptions/fdroid/` | `com/quantum/wallet/subscriptions/fdroid/` |
| `io/horizontalsystems/subscriptions/googleplay/` | `com/quantum/wallet/subscriptions/googleplay/` |

These moves apply to ALL source sets: `main`, `test`, `androidTest`, and flavor variants.

## CRITICAL — External Dependencies (DO NOT RENAME)

The following `import` statements reference **external third-party libraries** and must **NOT** be changed:

```
io.horizontalsystems.bitcoinkit.*
io.horizontalsystems.bitcoincash.*
io.horizontalsystems.litecoinkit.*
io.horizontalsystems.dashkit.*
io.horizontalsystems.ethereumkit.*
io.horizontalsystems.erc20kit.*
io.horizontalsystems.uniswapkit.*
io.horizontalsystems.oneinchkit.*
io.horizontalsystems.nftkit.*
io.horizontalsystems.solanakit.*
io.horizontalsystems.tronkit.*
io.horizontalsystems.tonkit.*
io.horizontalsystems.zcashbinancekt.*
io.horizontalsystems.monerokit.*
io.horizontalsystems.stellarkit.*
io.horizontalsystems.marketkit.*
io.horizontalsystems.feeratekit.*
io.horizontalsystems.hdwalletkit.*
io.horizontalsystems.pin.*
```

**Rule**: If an import starts with `io.horizontalsystems.` and the next segment is NOT one of `bankwallet`, `core`, `chartview`, `icons`, or `subscriptions`, leave it alone.

## Brand Name Mapping

| Old | New |
|-----|-----|
| `Unstoppable` (standalone app name) | `Quantum Wallet` |
| `Unstoppable Dev` | `Quantum Wallet Dev` |
| `Unstoppable Release` | `Quantum Wallet Release` |
| `Fdroid Unstoppable` | `Fdroid Quantum Wallet` |
| `Unstoppable Crypto Wallet` (store title) | `Quantum Crypto Wallet` |
| `Horizontal Systems` | `Quantum Chain PTE Ltd.` |
| `HorizontalSystems` (in company context) | `Quantum Chain PTE Ltd.` |

## Deeplink Scheme Mapping

| Old Scheme | New Scheme |
|------------|------------|
| `unstoppable` | `quantum` |
| `unstoppable-dev` | `quantum-dev` |
| `unstoppable-beta-debug` | `quantum-beta-debug` |
| `unstoppable-beta-release` | `quantum-beta-release` |

## URL Handling

Old URLs should be replaced with TODO placeholders since new URLs are not yet finalized:

| Old URL | Replacement |
|---------|-------------|
| `https://horizontalsystems.io` | `TODO("Replace with Quantum Chain website URL")` |
| `https://unstoppable.money` | `TODO("Replace with Quantum Wallet website URL")` |
| `https://unstoppable.money/analytics` | `TODO("Replace with Quantum Wallet analytics URL")` |
| `https://github.com/horizontalsystems/unstoppable-wallet-android` | `TODO("Replace with Quantum Wallet GitHub URL")` |
| `https://twitter.com/UnstoppableByHS` | `TODO("Replace with Quantum Wallet Twitter URL")` |
| `https://t.me/unstoppable_announcements` | `TODO("Replace with Quantum Wallet Telegram URL")` |
| `support.unstoppable@protonmail.com` | `TODO("Replace with Quantum Chain support email")` |

For string resource values in `build.gradle.kts`, use the TODO string directly as the value.
For Kotlin/Java code, use `// TODO: Replace with Quantum Chain URL` comments next to the old URL.

## Files That Need Changes (by category)

### Build Configuration
- `settings.gradle.kts` — rootProject.name
- `app/build.gradle.kts` — namespace, applicationId, resValue URLs, buildConfigField
- `core/build.gradle.kts` — namespace
- `components/icons/build.gradle.kts` — namespace
- `components/chartview/build.gradle.kts` — namespace
- `subscriptions-core/build.gradle.kts` — namespace
- `subscriptions-dev/build.gradle.kts` — namespace
- `subscriptions-fdroid/build.gradle.kts` — namespace
- `subscriptions-google-play/build.gradle.kts` — namespace

### Source Code (Kotlin)
- All `.kt` files under `app/src/main/java/io/horizontalsystems/bankwallet/`
- All `.kt` files under `core/src/main/java/io/horizontalsystems/core/`
- All `.kt` files under `components/icons/src/main/java/io/horizontalsystems/icons/`
- All `.kt` files under `components/chartview/src/main/java/io/horizontalsystems/chartview/`
- All `.kt` files under `subscriptions-*/src/main/java/io/horizontalsystems/subscriptions/`
- Test files under `app/src/test/java/io/horizontalsystems/`
- Instrumentation tests under `app/src/androidTest/java/io/horizontalsystems/`

### Android Resources & XML
- `app/src/main/AndroidManifest.xml` — activity/provider class references, deeplink schemes
- `app/src/main/res/navigation/main_graph.xml` — 200+ fragment class references
- `app/src/main/res/xml/appwidget_provider_info.xml` — widget class reference
- Flavor `strings.xml` files (6 variants) — App_Name, DeeplinkScheme
- Localized `strings.xml` files (11 locales) — user-facing "Unstoppable" references

### Store Metadata
- `fastlane/metadata/` — title.txt, full_description.txt, short_description.txt (14 locales)

### Documentation
- `README.md`, `CONTRIBUTE.md`, `RELEASE.md`, `CLAUDE.md`

### CI/CD
- `docker/build-apk.sh`
- `.github/workflows/*.yml` (3 files)
- `crowdin.yml` (if it has brand references)

## Verification Checklist

After completing the rebrand:
1. `grep -r "io\.horizontalsystems\.\(bankwallet\|core\|chartview\|icons\|subscriptions\)" --include="*.kt" --include="*.kts" --include="*.xml"` → should return zero results
2. `grep -ri "unstoppable" --include="*.xml" --include="*.kt" --include="*.kts" --include="*.txt"` → should return zero results (excluding comments/docs about migration)
3. `./gradlew assembleDebug` → builds successfully
4. `./gradlew test` → tests pass
