package io.horizontalsystems.bankwallet.entities

data class InitialSyncSetting(val coinType: CoinType,
                              var syncMode: SyncMode)
