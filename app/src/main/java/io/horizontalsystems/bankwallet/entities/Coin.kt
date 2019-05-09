package io.horizontalsystems.bankwallet.entities

import java.io.Serializable

data class Coin(val title: String, val code: String, val type: CoinType) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (other is Coin) {
            return title == other.title && code == other.code
        }

        return super.equals(other)
    }

    override fun hashCode(): Int {
        return title.hashCode() * 31 + code.hashCode()
    }
}
