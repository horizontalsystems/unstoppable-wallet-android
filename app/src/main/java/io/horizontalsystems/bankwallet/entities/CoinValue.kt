package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.PlatformCoin
import java.math.BigDecimal
import java.math.BigInteger

data class CoinValue(val platformCoin: PlatformCoin, val value: BigDecimal) {
    val coin = platformCoin.coin
    val decimal = platformCoin.decimals

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return platformCoin.coin.uid == other.platformCoin.coin.uid && value == other.value
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = platformCoin.coin.uid.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    fun getFormattedFull(): String {
        return App.numberFormatter.formatCoinFull(value, coin.code, 8)
    }
}

fun BigDecimal.isMaxValue(decimals: Int): Boolean {
    //biggest number in Ethereum platform
    val max256BitsNumber = BigInteger(
        "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
        16
    ).toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
    return this == max256BitsNumber
}
