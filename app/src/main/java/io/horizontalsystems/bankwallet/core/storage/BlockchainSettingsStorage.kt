package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.*

class BlockchainSettingsStorage(appDatabase: AppDatabase) {

    companion object {
        const val keyBtcRestore: String = "btc-restore"
        const val keyBtcTransactionSort: String = "btc-transaction-sort"
        const val keyEvmSyncSource: String = "evm-sync-source"
    }

    private val dao = appDatabase.blockchainSettingDao()

    fun btcRestoreMode(btcBlockchain: BtcBlockchain): BtcRestoreMode? {
        return dao.getBlockchainSetting(btcBlockchain.raw, keyBtcRestore)?.let { storedSetting ->
            BtcRestoreMode.values().firstOrNull { it.raw == storedSetting.value }
        }
    }

    fun save(btcRestoreMode: BtcRestoreMode, btcBlockchain: BtcBlockchain) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = btcBlockchain.raw,
                key = keyBtcRestore,
                value = btcRestoreMode.raw
            )
        )
    }

    fun btcTransactionSortMode(btcBlockchain: BtcBlockchain): TransactionDataSortMode? {
        return dao.getBlockchainSetting(btcBlockchain.raw, keyBtcTransactionSort)
            ?.let { sortSetting ->
                TransactionDataSortMode.values().firstOrNull { it.raw == sortSetting.value }
            }
    }

    fun save(transactionDataSortMode: TransactionDataSortMode, btcBlockchain: BtcBlockchain) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = btcBlockchain.raw,
                key = keyBtcTransactionSort,
                value = transactionDataSortMode.raw
            )
        )
    }

    fun evmSyncSourceName(evmBlockchain: EvmBlockchain): String? {
        return dao.getBlockchainSetting(evmBlockchain.uid, keyEvmSyncSource)?.value
    }

    fun save(evmSyncSourceName: String, evmBlockchain: EvmBlockchain) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = evmBlockchain.uid,
                key = keyEvmSyncSource,
                value = evmSyncSourceName
            )
        )
    }

}
