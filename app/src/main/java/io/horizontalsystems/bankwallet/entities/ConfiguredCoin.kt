package io.horizontalsystems.bankwallet.entities

import android.os.Parcelable
import io.horizontalsystems.coinkit.models.Coin
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ConfiguredCoin(
        val coin: Coin,
        val settings: CoinSettings = CoinSettings()
) : Parcelable {

    override fun equals(other: Any?): Boolean {
        if (other is ConfiguredCoin) {
            return coin == other.coin && settings == other.settings
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return coin.hashCode()
    }

}
