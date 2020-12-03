package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class CoinRecord(
        @PrimaryKey val coinId: String,
        val title: String,
        val code: String,
        val decimal: Int,
        val tokenType: String,
        var erc20Address: String? = null,
        var bep2Symbol: String? = null)
