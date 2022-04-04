package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.storage.BlockchainSettingsStorage
import io.horizontalsystems.bankwallet.entities.AccountOrigin
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.entities.BtcRestoreMode
import io.horizontalsystems.bankwallet.entities.TransactionDataSortMode
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.marketkit.models.CoinType
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BtcBlockchainManager(
    private val storage: BlockchainSettingsStorage
) {

    private val restoreModeUpdatedSubject = PublishSubject.create<BtcBlockchain>()
    val restoreModeUpdatedObservable: Observable<BtcBlockchain> = restoreModeUpdatedSubject

    private val transactionSortModeUpdatedSubject = PublishSubject.create<BtcBlockchain>()
    val transactionSortModeUpdatedObservable: Observable<BtcBlockchain> =
        transactionSortModeUpdatedSubject


    fun blockchain(coinType: CoinType): BtcBlockchain? {
        return BtcBlockchain.values().firstOrNull { it.supports(coinType) }
    }

    fun restoreMode(blockchain: BtcBlockchain): BtcRestoreMode {
        return storage.btcRestoreMode(blockchain) ?: BtcRestoreMode.Api
    }

    fun syncMode(blockchain: BtcBlockchain, accountOrigin: AccountOrigin): BitcoinCore.SyncMode {
        if (accountOrigin == AccountOrigin.Created) {
            return BitcoinCore.SyncMode.NewWallet()
        }

        return when (restoreMode(blockchain)) {
            BtcRestoreMode.Api -> BitcoinCore.SyncMode.Api()
            BtcRestoreMode.Blockchain -> BitcoinCore.SyncMode.Full()
        }
    }

    fun save(restoreMode: BtcRestoreMode, blockchain: BtcBlockchain) {
        storage.save(restoreMode, blockchain)
        restoreModeUpdatedSubject.onNext(blockchain)
    }

    fun transactionSortMode(blockchain: BtcBlockchain): TransactionDataSortMode {
        return storage.btcTransactionSortMode(blockchain) ?: TransactionDataSortMode.Shuffle
    }

    fun save(transactionSortMode: TransactionDataSortMode, blockchain: BtcBlockchain) {
        storage.save(transactionSortMode, blockchain)
        transactionSortModeUpdatedSubject.onNext(blockchain)
    }
}
