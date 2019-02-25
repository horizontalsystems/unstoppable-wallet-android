package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.CoinTypeConverter
import java.io.Serializable

sealed class CoinType : Serializable {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Ethereum : CoinType()
    class Erc20(val address: String, val decimal: Int) : CoinType()
}

data class Coin(val title: String, val code: String, val type: CoinType) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (other is Coin) {
            return title == other.title
                    && code == other.code
                    && type == other.type
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + code.hashCode()
        result = 31 * result + type.hashCode()
        return result
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
