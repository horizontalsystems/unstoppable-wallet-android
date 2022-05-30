package io.horizontalsystems.bankwallet.modules.settings.security.blockchains

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.BtcBlockchain
import io.horizontalsystems.bankwallet.modules.settings.security.blockchains.BlockchainSettingsModule.BlockchainItem
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class BlockchainSettingsService(
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var blockchainItems: List<BlockchainItem> = listOf()
        private set(value) {
            field = value
            blockchainItemsSubject.onNext(value)
        }

    private val blockchainItemsSubject = BehaviorSubject.create<List<BlockchainItem>>()
    val blockchainItemsObservable: Observable<List<BlockchainItem>>
        get() = blockchainItemsSubject


    fun start() {
        btcBlockchainManager.restoreModeUpdatedObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        btcBlockchainManager.transactionSortModeUpdatedObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        evmSyncSourceManager.syncSourceObservable
            .subscribeIO {
                syncBlockchainItems()
            }.let {
                disposables.add(it)
            }

        syncBlockchainItems()
    }

    fun stop() {
        disposables.clear()
    }

    private fun syncBlockchainItems() {
        val btcBlockchainItems = BtcBlockchain.values().map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain)
            val transactionMode = btcBlockchainManager.transactionSortMode(blockchain)
            BlockchainItem.Btc(blockchain, restoreMode, transactionMode)
        }

        val evmBlockchainItems = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain)
            BlockchainItem.Evm(blockchain, syncSource)
        }

        blockchainItems = (btcBlockchainItems + evmBlockchainItems).sortedBy { it.order }
    }

}
