package io.horizontalsystems.walletkit.modules.blockchainsettings

import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import io.horizontalsystems.marketkit.models.BlockchainType
import io.reactivex.Observable
import kotlinx.coroutines.flow.catch
import timber.log.Timber
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BlockchainSettingsService(
    private val evmBlockchainManager: EvmBlockchainManager,
    private val evmSyncSourceManager: EvmSyncSourceManager,
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
            evmSyncSourceManager.syncSourceFlow.collect {
                syncBlockchainItems()
            }
        }
        ChainRegistry.all.forEach { plugin ->
            plugin.settingsRefreshTrigger?.let { trigger ->
                coroutineScope.launch {
                    trigger
                        .catch { Timber.e(it, "Chain plugin %s trigger failed", plugin.blockchainType.uid) }
                        .collect {
                            try {
                                syncBlockchainItems()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
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

    // collectors run on independent coroutines; the lock keeps an older
    // snapshot from being published after a newer one
    private val syncMutex = Mutex()

    private suspend fun syncBlockchainItems() = syncMutex.withLock {
        val evmBlockchainItems = evmBlockchainManager.allBlockchains.map { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(blockchain.type)
            BlockchainItem.Evm(blockchain, syncSource)
        }

        val chainBlockchainItems = ChainRegistry.all.mapNotNull { it.blockchainSettingsItem() }

        blockchainItems = (evmBlockchainItems + chainBlockchainItems).sortedBy { it.order }
    }

}
