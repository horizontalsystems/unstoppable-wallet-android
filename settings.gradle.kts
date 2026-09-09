pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        // horizontalsystems build of the Zodl Zcash SDK (branch hs/3.1.x, Slipstream enabled).
        // Pinned to the commit of the fork's `maven` branch that published the current
        // zcashSdk version, so the source is immutable; bump the commit together with the version.
        maven {
            url = uri("https://raw.githubusercontent.com/horizontalsystems/zodl-android-wallet-sdk/8f6c8661321543317cb3c7d09d57d4e2bdddad52/")
            content { includeGroup("com.zodl.android") }
        }
    }
}

rootProject.name = "Unstoppable"

include(":app")
include(":walletkit")
include(":walletkit-chain-zano")
include(":walletkit-chain-monero")
include(":walletkit-chain-zcash")
include(":walletkit-chain-solana")
include(":walletkit-chain-stellar")
include(":walletkit-chain-ton")
include(":walletkit-chain-tron")
include(":walletkit-chain-thorchain")
include(":walletkit-chain-bitcoin")
include(":walletkit-chain-evm")
include(":components:icons")
include(":components:chartview")
include(":subscriptions-core")
if (file("subscriptions-google-play").exists()) {
    include(":subscriptions-google-play")
}
include(":subscriptions-dev")
include(":subscriptions-fdroid")

include(":dapp-core")
if (file("dapp-wallet-connect").exists()) {
    include(":dapp-wallet-connect")
}
