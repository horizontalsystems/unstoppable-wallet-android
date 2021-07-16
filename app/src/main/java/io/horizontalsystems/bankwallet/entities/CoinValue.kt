package io.horizontalsystems.bankwallet.entities

import android.util.Log
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.coinkit.models.Coin
import java.math.BigDecimal
import java.math.BigInteger

data class CoinValue(val coin: Coin, val value: BigDecimal) {

    val isMaxValue: Boolean
        get() {
            //biggest number in Ethereum platform
            val max256BitsNumber = BigInteger(
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                16
            ).toBigDecimal().movePointLeft(coin.decimal).stripTrailingZeros()
            Log.e("TAG", "max: $max256BitsNumber ")
            Log.e("TAG", "val: $value ")
            return value == max256BitsNumber
        }

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
