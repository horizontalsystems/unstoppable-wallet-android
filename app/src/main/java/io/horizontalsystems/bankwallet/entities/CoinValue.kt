package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.App
import java.math.BigDecimal
import java.math.BigInteger

data class CoinValue(
    val kind: Kind,
    val value: BigDecimal,
) {

    sealed class Kind {
        abstract val decimal: Int
        abstract val coin: io.horizontalsystems.marketkit.models.Coin

        data class PlatformCoin(val platformCoin: io.horizontalsystems.marketkit.models.PlatformCoin) : Kind() {
            override val decimal: Int
                get() = platformCoin.platform.decimals
            override val coin: io.horizontalsystems.marketkit.models.Coin
                get() = platformCoin.coin
        }

        data class Coin(override val coin: io.horizontalsystems.marketkit.models.Coin, override val decimal: Int) : Kind()
    }

    val isMaxValue: Boolean
        get() {
            //biggest number in Ethereum platform
            val max256BitsNumber = BigInteger(
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                16
            ).toBigDecimal().movePointLeft(kind.decimal).stripTrailingZeros()
            return value == max256BitsNumber
        }

    val abs
        get() = CoinValue(kind, value.abs())

    val coin
        get() = kind.coin

    val decimal
        get() = kind.decimal

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return kind == other.kind && value == other.value
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    fun getFormatted(): String {
        return App.numberFormatter.formatCoin(value, coin.code, 0, 8)
    }

}
