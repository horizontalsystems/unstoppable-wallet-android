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
