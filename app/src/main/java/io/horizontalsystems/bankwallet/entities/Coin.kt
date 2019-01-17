package io.horizontalsystems.bankwallet.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import android.arch.persistence.room.TypeConverters
import io.horizontalsystems.bankwallet.core.storage.CoinTypeConverter

@Entity
@TypeConverters(CoinTypeConverter::class)
data class Coin(
        val title: String,
        @PrimaryKey val code: String,
        var enabled: Boolean,
        val type: CoinType,
        var order: Int? = null)

sealed class CoinType {
    object BitcoinCash : CoinType()
    object Bitcoin : CoinType()
    object Ethereum : CoinType()
    class Erc20(val address: String, val decimal: Int) : CoinType()
}
