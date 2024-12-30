package cash.p.terminal.core.storage

import cash.p.terminal.entities.BlockchainSettingRecord
import cash.p.terminal.entities.BtcRestoreMode
import cash.p.terminal.entities.TransactionDataSortMode
import io.horizontalsystems.core.entities.BlockchainType

class BlockchainSettingsStorage(appDatabase: AppDatabase) {

    companion object {
        const val keyBtcRestore: String = "btc-restore"
        const val keyBtcTransactionSort: String = "btc-transaction-sort"
        const val keyEvmSyncSourceUrl: String = "evm-sync-source-url"
    }

    private val dao by lazy { appDatabase.blockchainSettingDao() }

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

    fun evmSyncSourceUrl(blockchainType: BlockchainType): String? {
        return dao.getBlockchainSetting(blockchainType.uid, keyEvmSyncSourceUrl)?.value
    }

    fun save(evmSyncSourceUrl: String, blockchainType: BlockchainType) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = blockchainType.uid,
                key = keyEvmSyncSourceUrl,
                value = evmSyncSourceUrl
            )
        )
    }

}
