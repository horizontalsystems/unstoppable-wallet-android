package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.BlockchainSettingRecord
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.BlockchainType

@javax.inject.Singleton
class BlockchainSettingsStorage @javax.inject.Inject constructor(appDatabase: AppDatabase) {

    companion object {
        const val keyBtcRestore: String = "btc-restore"
        const val keyBtcTransactionSort: String = "btc-transaction-sort"
        const val keyEvmSyncSourceUrl: String = "evm-sync-source-url"
        const val keyMoneroNode: String = "monero-node"
        const val keyZanoNode: String = "zano-node"
        const val keyZcashEndpoint: String = "zcash-endpoint"
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

    fun moneroNodeHost(): String? {
        return dao.getBlockchainSetting(BlockchainType.Monero.uid, keyMoneroNode)?.value
    }

    fun saveMoneroNode(host: String) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = BlockchainType.Monero.uid,
                key = keyMoneroNode,
                value = host
            )
        )
    }

    fun zanoNodeHost(): String? =
        dao.getBlockchainSetting(BlockchainType.Zano.uid, keyZanoNode)?.value

    fun saveZanoNode(host: String) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = BlockchainType.Zano.uid,
                key = keyZanoNode,
                value = host
            )
        )
    }

    fun zcashEndpointUrl(): String? =
        dao.getBlockchainSetting(BlockchainType.Zcash.uid, keyZcashEndpoint)?.value

    fun saveZcashEndpoint(url: String) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = BlockchainType.Zcash.uid,
                key = keyZcashEndpoint,
                value = url
            )
        )
    }

}
