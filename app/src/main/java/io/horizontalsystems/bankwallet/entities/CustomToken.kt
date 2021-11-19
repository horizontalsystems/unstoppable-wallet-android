package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.Platform
import io.horizontalsystems.marketkit.models.PlatformCoin

@Entity
data class CustomToken(
    val coinName: String,
    val coinCode: String,
    @PrimaryKey
    val coinType: CoinType,
    val decimal: Int
) {
    val platformCoin: PlatformCoin
        get() {
            val coinUid = "${uidPrefix}${coinName}_${coinCode}"
            return PlatformCoin(Platform(coinType, decimal, coinUid), Coin(coinUid, coinName, coinCode))
        }

    companion object {
        const val uidPrefix = "custom_"
    }
}
