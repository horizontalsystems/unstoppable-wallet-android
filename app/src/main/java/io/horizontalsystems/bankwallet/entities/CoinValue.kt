package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.math.BigInteger

data class CoinValue(val coin: Coin, val decimal: Int, val value: BigDecimal) {

    constructor(token: Token, value: BigDecimal) : this(token.coin, token.decimals, value)

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return coin.uid == other.coin.uid && value == other.value
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = coin.uid.hashCode()
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
