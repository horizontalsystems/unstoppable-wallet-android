package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.BlockchainSettingRecord
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.BlockchainType

class BlockchainSettingsStorage(appDatabase: AppDatabase) {

    companion object {
        const val keyBtcRestore: String = "btc-restore"
        const val keyBtcTransactionSort: String = "btc-transaction-sort"
        const val keyEvmSyncSource: String = "evm-sync-source"
    }

    private val dao = appDatabase.blockchainSettingDao()

    fun btcRestoreMode(blockchainType: BlockchainType): BtcRestoreMode? {
        return dao.getBlockchainSetting(blockchainType.uid, keyBtcRestore)?.let { storedSetting ->
            BtcRestoreMode.values().firstOrNull { it.raw == storedSetting.value }
        }
    }

    fun save(btcRestoreMode: BtcRestoreMode, blockchainType: BlockchainType) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = blockchainType.uid,
                key = keyBtcRestore,
                value = btcRestoreMode.raw
            )
        )
    }

    fun btcTransactionSortMode(blockchainType: BlockchainType): TransactionDataSortMode? {
        return dao.getBlockchainSetting(blockchainType.uid, keyBtcTransactionSort)
            ?.let { sortSetting ->
                TransactionDataSortMode.values().firstOrNull { it.raw == sortSetting.value }
            }
    }

    fun save(transactionDataSortMode: TransactionDataSortMode, blockchainType: BlockchainType) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = blockchainType.uid,
                key = keyBtcTransactionSort,
                value = transactionDataSortMode.raw
            )
        )
    }

    fun evmSyncSourceName(blockchainType: BlockchainType): String? {
        return dao.getBlockchainSetting(blockchainType.uid, keyEvmSyncSource)?.value
    }

    fun save(evmSyncSourceName: String, blockchainType: BlockchainType) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = blockchainType.uid,
                key = keyEvmSyncSource,
                value = evmSyncSourceName
            )
        )
    }

}
