package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.CoinTypeConverter
import java.io.Serializable

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Dash : CoinType()
    object Ethereum : CoinType()
    class Erc20(val address: String, val decimal: Int) : CoinType()
}

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


@Entity
@TypeConverters(CoinTypeConverter::class)
data class StorableCoin(
        @PrimaryKey
        val coinCode: String,
        var coinTitle: String,
        val coinType: CoinType,
        var enabled: Boolean,
        var order: Int? = null)
