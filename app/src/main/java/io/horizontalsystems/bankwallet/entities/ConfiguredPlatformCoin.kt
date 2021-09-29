package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConfiguredPlatformCoin(
    val platformCoin: PlatformCoin,
    val coinSettings: CoinSettings = CoinSettings()
): Parcelable {
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
