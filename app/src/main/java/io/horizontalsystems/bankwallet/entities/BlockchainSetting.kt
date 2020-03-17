package io.horizontalsystems.bankwallet.entities

import androidx.room.Entity

@Entity(primaryKeys = ["coinType"])
data class BlockchainSetting(
        val coinType: CoinType,
        var derivation: AccountType.Derivation?,
        var syncMode: SyncMode?)