package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.BlockchainSettingRecord
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.xxxkit.models.BlockchainType

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
