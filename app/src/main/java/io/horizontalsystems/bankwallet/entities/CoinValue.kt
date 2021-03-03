package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.Coin
import java.math.BigDecimal

data class CoinValue(val coin: Coin, val value: BigDecimal) {

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return coin.code == other.coin.code && value == other.value

        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = coin.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

    fun getFormatted(): String {
        return App.numberFormatter.formatCoin(value, coin.code, 0, 8)
    }

}
