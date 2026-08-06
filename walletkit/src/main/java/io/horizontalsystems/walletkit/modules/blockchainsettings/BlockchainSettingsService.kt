package io.horizontalsystems.walletkit.modules.blockchainsettings

import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSourceManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import kotlinx.coroutines.flow.catch
import timber.log.Timber
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
    private val thorchainRpcSourceManager: ThorchainRpcSourceManager,
    private val marketKit: MarketKitWrapper
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
            thorchainRpcSourceManager.rpcSourceUpdatedFlow.collect {
                syncBlockchainItems()
            }
        }
        ChainRegistry.all.forEach { plugin ->
            plugin.walletReloadTrigger?.let { trigger ->
                coroutineScope.launch {
                    trigger
                        .catch { Timber.e(it, "Chain plugin %s trigger failed", plugin.blockchainType.uid) }
                        .collect {
                            try {
                                syncBlockchainItems()
                            } catch (e: Throwable) {
                                Timber.e(e, "Syncing blockchain settings items failed")
                            }
                        }
                }
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

        val tronBlockchainItems = mutableListOf<BlockchainItem>()
        marketKit.blockchain(BlockchainType.Tron.uid)?.let { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(BlockchainType.Tron)
            tronBlockchainItems.add(BlockchainItem.Evm(blockchain, syncSource))
        }

        val thorchainBlockchainItems = mutableListOf<BlockchainItem>()
        thorchainRpcSourceManager.blockchain?.let {
            thorchainBlockchainItems.add(BlockchainItem.Thorchain(it, thorchainRpcSourceManager.rpcSource))
        }

        val chainBlockchainItems = ChainRegistry.all.mapNotNull { it.blockchainSettingsItem() }

        blockchainItems = (btcBlockchainItems + evmBlockchainItems + tronBlockchainItems + thorchainBlockchainItems + chainBlockchainItems).sortedBy { it.order }
    }

}
