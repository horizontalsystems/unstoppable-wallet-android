package io.horizontalsystems.bankwallet.entities

import io.horizontalsystems.bankwallet.entities.coins.Coin

data class CoinValue(val coin: Coin, val value: Double) {

    override fun equals(other: Any?): Boolean {
        if (other is CoinValue) {
            return coin == other.coin && value == other.value

        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = coin.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }

}
