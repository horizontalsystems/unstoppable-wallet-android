package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.marketkit.models.PlatformCoin
import kotlinx.android.parcel.Parcelize
import java.util.*

@Parcelize
data class ConfiguredPlatformCoin(
    val platformCoin: PlatformCoin,
    val coinSettings: CoinSettings = CoinSettings()
): Parcelable {
    override fun hashCode(): Int {
        return Objects.hash(platformCoin, coinSettings)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is ConfiguredPlatformCoin &&
                other.platformCoin == platformCoin &&
                other.coinSettings == coinSettings
    }
}
