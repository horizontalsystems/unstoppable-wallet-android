package io.horizontalsystems.bankwallet.entities

data class SyncModeSetting(val coinType: CoinType,
                           var syncMode: SyncMode)
