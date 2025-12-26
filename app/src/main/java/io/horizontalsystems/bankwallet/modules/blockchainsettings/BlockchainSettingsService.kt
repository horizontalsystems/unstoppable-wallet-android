package io.horizontalsystems.bankwallet.modules.blockchainsettings

import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BlockchainSettingsService(
    private val btcBlockchainManager: BtcBlockchainManager,
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
    private val solanaRpcSourceManager: SolanaRpcSourceManager,
    private val moneroNodeManager: MoneroNodeManager
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    var blockchainItems: List<BlockchainItem> = listOf()
        private set(value) {
            field = value
            blockchainItemsSubject.onNext(value)
        }

    private val blockchainItemsSubject = BehaviorSubject.create<List<BlockchainItem>>()
    val blockchainItemsObservable: Observable<List<BlockchainItem>>
        get() = blockchainItemsSubject


    fun start() {
        coroutineScope.launch {
            btcBlockchainManager.restoreModeUpdatedObservable.asFlow().collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            btcBlockchainManager.transactionSortModeUpdatedObservable.asFlow().collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            evmSyncSourceManager.syncSourceObservable.asFlow().collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            solanaRpcSourceManager.rpcSourceUpdateObservable.asFlow().collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            moneroNodeManager.currentNodeUpdatedFlow.collect {
                syncBlockchainItems()
            }
        }

        coroutineScope.launch {
            syncBlockchainItems()
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    private fun syncBlockchainItems() {
        val btcBlockchainItems = btcBlockchainManager.allBlockchains.map { blockchain ->
            val restoreMode = btcBlockchainManager.restoreMode(blockchain.type)
            BlockchainItem.Btc(blockchain, restoreMode)
        }

        val evmBlockchainItems = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            BlockchainItem.Evm(blockchain, syncSource)
        }

        val solanaBlockchainItems = mutableListOf<BlockchainItem>()

        solanaRpcSourceManager.blockchain?.let {
            solanaBlockchainItems.add(BlockchainItem.Solana(it, solanaRpcSourceManager.rpcSource))
        }

        val moneroBlockchainItems = mutableListOf<BlockchainItem>()
        moneroNodeManager.blockchain?.let {
            moneroBlockchainItems.add(BlockchainItem.Monero(it, moneroNodeManager.currentNode))
        }

        blockchainItems = (btcBlockchainItems +  evmBlockchainItems + solanaBlockchainItems + moneroBlockchainItems).sortedBy { it.order }
    }

}
