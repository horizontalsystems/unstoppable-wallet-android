package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BtcBlockchainManager(
    private val storage: BlockchainSettingsStorage,
    marketKit: MarketKitWrapper
) {

    private val restoreModeUpdatedSubject = PublishSubject.create<BlockchainType>()
    val restoreModeUpdatedObservable: Observable<BlockchainType> = restoreModeUpdatedSubject

    private val transactionSortModeUpdatedSubject = PublishSubject.create<BlockchainType>()
    val transactionSortModeUpdatedObservable: Observable<BlockchainType> =
        transactionSortModeUpdatedSubject

    private val blockchainTypes = listOf(
        BlockchainType.Bitcoin,
        BlockchainType.BitcoinCash,
        BlockchainType.Litecoin,
        BlockchainType.Dash,
        BlockchainType.ECash,
    )

    val allBlockchains = marketKit.blockchains(blockchainTypes.map { it.uid })

    fun blockchain(blockchainType: BlockchainType) =
        allBlockchains.firstOrNull { blockchainType == it.type }

    fun restoreMode(blockchainType: BlockchainType): BtcRestoreMode {
        return storage.btcRestoreMode(blockchainType) ?: BtcRestoreMode.Api
    }

    fun syncMode(blockchainType: BlockchainType, accountOrigin: AccountOrigin): BitcoinCore.SyncMode {
        if (accountOrigin == AccountOrigin.Created) {
            return BitcoinCore.SyncMode.NewWallet()
        }

        return when (restoreMode(blockchainType)) {
            BtcRestoreMode.Api -> BitcoinCore.SyncMode.Api()
            BtcRestoreMode.Blockchain -> BitcoinCore.SyncMode.Full()
        }
    }

    fun save(restoreMode: BtcRestoreMode, blockchainType: BlockchainType) {
        storage.save(restoreMode, blockchainType)
        restoreModeUpdatedSubject.onNext(blockchainType)
    }

    fun transactionSortMode(blockchainType: BlockchainType): TransactionDataSortMode {
        return storage.btcTransactionSortMode(blockchainType) ?: TransactionDataSortMode.Shuffle
    }

    fun save(transactionSortMode: TransactionDataSortMode, blockchainType: BlockchainType) {
        storage.save(transactionSortMode, blockchainType)
        transactionSortModeUpdatedSubject.onNext(blockchainType)
    }
}
