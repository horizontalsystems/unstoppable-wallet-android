package com.quantum.wallet.bankwallet.modules.walletconnect.storage

import androidx.room.Entity

@Entity(primaryKeys = ["accountId", "topic"])
data class WalletConnectV2Session(
        val accountId: String,
        val topic: String,
)
