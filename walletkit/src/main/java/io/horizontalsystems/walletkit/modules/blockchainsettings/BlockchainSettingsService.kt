package io.horizontalsystems.walletkit.modules.blockchainsettings

import io.horizontalsystems.walletkit.core.managers.BtcBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.managers.EvmSyncSourceManager
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.core.managers.MoneroNodeManager
import io.horizontalsystems.walletkit.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.walletkit.core.managers.ThorchainRpcSourceManager
import io.horizontalsystems.walletkit.core.chain.ChainRegistry
import io.horizontalsystems.walletkit.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.walletkit.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import io.horizontalsystems.marketkit.models.BlockchainType
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
    private val thorchainRpcSourceManager: ThorchainRpcSourceManager,
    private val moneroNodeManager: MoneroNodeManager,
    private val zcashEndpointManager: ZcashLightWalletEndpointManager,
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
            solanaRpcSourceManager.rpcSourceUpdateObservable.asFlow().collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            thorchainRpcSourceManager.rpcSourceUpdatedFlow.collect {
                syncBlockchainItems()
            }
        }
        coroutineScope.launch {
            moneroNodeManager.currentNodeUpdatedFlow.collect {
                syncBlockchainItems()
            }
        }
        ChainRegistry.all.forEach { plugin ->
            plugin.walletReloadTrigger?.let { trigger ->
                coroutineScope.launch {
                    trigger.collect {
                        syncBlockchainItems()
                    }
                }
            }
        }
        coroutineScope.launch {
            zcashEndpointManager.currentEndpointUpdatedFlow.collect {
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

        val tronBlockchainItems = mutableListOf<BlockchainItem>()
        marketKit.blockchain(BlockchainType.Tron.uid)?.let { blockchain ->
            val syncSource = evmSyncSourceManager.getSyncSource(BlockchainType.Tron)
            tronBlockchainItems.add(BlockchainItem.Evm(blockchain, syncSource))
        }

        val solanaBlockchainItems = mutableListOf<BlockchainItem>()
        solanaRpcSourceManager.blockchain?.let {
            solanaBlockchainItems.add(BlockchainItem.Solana(it, solanaRpcSourceManager.rpcSource))
        }

        val thorchainBlockchainItems = mutableListOf<BlockchainItem>()
        thorchainRpcSourceManager.blockchain?.let {
            thorchainBlockchainItems.add(BlockchainItem.Thorchain(it, thorchainRpcSourceManager.rpcSource))
        }

        val moneroBlockchainItems = mutableListOf<BlockchainItem>()
        moneroNodeManager.blockchain?.let {
            moneroBlockchainItems.add(BlockchainItem.Monero(it, moneroNodeManager.currentNode))
        }

        val chainBlockchainItems = ChainRegistry.all.mapNotNull { it.blockchainSettingsItem() }

        val zcashBlockchainItems = mutableListOf<BlockchainItem>()
        zcashEndpointManager.blockchain?.let {
            zcashBlockchainItems.add(BlockchainItem.Zcash(it, zcashEndpointManager.currentEndpoint))
        }

        blockchainItems = (btcBlockchainItems + evmBlockchainItems + tronBlockchainItems + solanaBlockchainItems + thorchainBlockchainItems + moneroBlockchainItems + chainBlockchainItems + zcashBlockchainItems).sortedBy { it.order }
    }

}
