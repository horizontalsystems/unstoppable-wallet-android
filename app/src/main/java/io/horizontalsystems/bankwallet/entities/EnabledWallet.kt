package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["coinCode", "accountName"])
data class EnabledWallet(
        val coinCode: String,
        var walletOrder: Int? = null,
        val accountName: String,
        val syncMode: SyncMode
)
