package io.horizontalsystems.walletkit.core.managers

import io.horizontalsystems.walletkit.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.walletkit.entities.AccountOrigin
import io.horizontalsystems.walletkit.entities.BtcRestoreMode
import io.horizontalsystems.walletkit.entities.TransactionDataSortMode
import io.horizontalsystems.marketkit.models.BlockchainType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BtcBlockchainManager(
    private val storage: BlockchainSettingsStorage,
    marketKit: MarketKitWrapper,
) {

    // Unbounded, matching the Rx BUFFER strategy this replaced: each event names a
    // blockchain whose setting changed, so no event may be dropped.
    private val _restoreModeUpdatedFlow =
        MutableSharedFlow<BlockchainType>(extraBufferCapacity = Int.MAX_VALUE)
    val restoreModeUpdatedFlow: Flow<BlockchainType> = _restoreModeUpdatedFlow

    private val _transactionSortModeUpdatedFlow =
        MutableSharedFlow<BlockchainType>(extraBufferCapacity = Int.MAX_VALUE)
    val transactionSortModeUpdatedFlow: Flow<BlockchainType> = _transactionSortModeUpdatedFlow

    private val blockchairSyncEnabledBlockchains = listOf(BlockchainType.Bitcoin, BlockchainType.BitcoinCash, BlockchainType.Litecoin)

    val blockchainTypes by lazy {
        listOf(
            BlockchainType.Bitcoin,
            BlockchainType.BitcoinCash,
            BlockchainType.Litecoin,
            BlockchainType.Dash,
            BlockchainType.ECash,
        )
    }

    val allBlockchains by lazy { marketKit.blockchains(blockchainTypes.map { it.uid }) }

    fun blockchain(blockchainType: BlockchainType) =
        allBlockchains.firstOrNull { blockchainType == it.type }

    private fun defaultRestoreMode(blockchainType: BlockchainType) =
        if (blockchainType in blockchairSyncEnabledBlockchains) BtcRestoreMode.Blockchair else BtcRestoreMode.Hybrid

    fun restoreMode(blockchainType: BlockchainType): BtcRestoreMode {
        return storage.btcRestoreMode(blockchainType) ?: defaultRestoreMode(blockchainType)
    }

    fun availableRestoreModes(blockchainType: BlockchainType) =
        BtcRestoreMode.values().let {
            val values = it.toList()
            if (blockchainType !in blockchairSyncEnabledBlockchains) {
                values - BtcRestoreMode.Blockchair
            } else {
                values
            }
        }

    fun save(restoreMode: BtcRestoreMode, blockchainType: BlockchainType) {
        storage.save(restoreMode, blockchainType)
        _restoreModeUpdatedFlow.tryEmit(blockchainType)
    }

    fun transactionSortMode(blockchainType: BlockchainType): TransactionDataSortMode {
        return storage.btcTransactionSortMode(blockchainType) ?: TransactionDataSortMode.Shuffle
    }

    fun save(transactionSortMode: TransactionDataSortMode, blockchainType: BlockchainType) {
        storage.save(transactionSortMode, blockchainType)
        _transactionSortModeUpdatedFlow.tryEmit(blockchainType)
    }
}
