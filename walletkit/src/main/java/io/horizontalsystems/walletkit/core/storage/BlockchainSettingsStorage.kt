package io.horizontalsystems.walletkit.core.storage

import io.horizontalsystems.walletkit.entities.BlockchainSettingRecord
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.BlockchainType

class BlockchainSettingsStorage(appDatabase: AppDatabase) {

    companion object {
        const val keyBtcRestore: String = "btc-restore"
        const val keyBtcTransactionSort: String = "btc-transaction-sort"
        const val keyEvmSyncSourceUrl: String = "evm-sync-source-url"
        const val keyMoneroNode: String = "monero-node"
        const val keyMoneroAutoSelect: String = "monero-auto-select"
        const val keyZanoNode: String = "zano-node"
        const val keyZcashEndpoint: String = "zcash-endpoint"
        const val keyZcashAutoSelect: String = "zcash-auto-select"
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

    fun moneroAutoSelect(): Boolean {
        // Only an explicit "false" disables it: toBoolean() would read any malformed stored
        // value as false, silently turning the default-on feature off.
        return !dao.getBlockchainSetting(BlockchainType.Monero.uid, keyMoneroAutoSelect)?.value.equals("false", ignoreCase = true)
    }

    fun saveMoneroAutoSelect(enabled: Boolean) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = BlockchainType.Monero.uid,
                key = keyMoneroAutoSelect,
                value = enabled.toString()
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

    fun zcashAutoSelect(): Boolean {
        return !dao.getBlockchainSetting(BlockchainType.Zcash.uid, keyZcashAutoSelect)?.value.equals("false", ignoreCase = true)
    }

    fun saveZcashAutoSelect(enabled: Boolean) {
        dao.insert(
            BlockchainSettingRecord(
                blockchainUid = BlockchainType.Zcash.uid,
                key = keyZcashAutoSelect,
                value = enabled.toString()
            )
        )
    }

}
