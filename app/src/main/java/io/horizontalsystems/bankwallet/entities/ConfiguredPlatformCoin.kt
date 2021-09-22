package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.marketkit.models.PlatformCoin

data class ConfiguredPlatformCoin(val platformCoin: PlatformCoin, val coinSettings: CoinSettings = CoinSettings()) {
    override fun hashCode(): Int {
        return platformCoin.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ConfiguredPlatformCoin

        if (platformCoin != other.platformCoin) return false
        if (coinSettings != other.coinSettings) return false

        return true
    }
}