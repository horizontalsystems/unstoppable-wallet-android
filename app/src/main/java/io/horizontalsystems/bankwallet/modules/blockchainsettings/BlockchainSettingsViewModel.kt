package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.managers.BtcBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmBlockchainManager
import io.horizontalsystems.bankwallet.core.managers.EvmSyncSourceManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.core.managers.MoneroNodeManager
import io.horizontalsystems.bankwallet.core.managers.SolanaRpcSourceManager
import io.horizontalsystems.bankwallet.core.managers.ZanoNodeManager
import io.horizontalsystems.bankwallet.core.managers.ZcashLightWalletEndpointManager
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.providers.Translator
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

@HiltViewModel
class BlockchainSettingsViewModel @Inject constructor(
    btcBlockchainManager: BtcBlockchainManager,
    evmBlockchainManager: EvmBlockchainManager,
    evmSyncSourceManager: EvmSyncSourceManager,
    solanaRpcSourceManager: SolanaRpcSourceManager,
    moneroNodeManager: MoneroNodeManager,
    zanoNodeManager: ZanoNodeManager,
    zcashEndpointManager: ZcashLightWalletEndpointManager,
    marketKit: MarketKitWrapper,
) : ViewModel() {
    private val service = BlockchainSettingsService(
        btcBlockchainManager, evmBlockchainManager, evmSyncSourceManager,
        solanaRpcSourceManager, moneroNodeManager, zanoNodeManager, zcashEndpointManager, marketKit
    )

    var btcLikeChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    var otherChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    init {
        viewModelScope.launch {
            service.blockchainItemsObservable.asFlow().collect {
                sync(it)
            }
        }

        service.start()
        sync(service.blockchainItems)
    }

    override fun onCleared() {
        service.stop()
    }

    private fun sync(blockchainItems: List<BlockchainSettingsModule.BlockchainItem>) {
        viewModelScope.launch {
            val btcItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Btc>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = Translator.getString(item.restoreMode.title),
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            val moneroItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Monero>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.node.name,
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            val zanoItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Zano>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.node.name,
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            val zcashItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Zcash>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.endpoint.name,
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            btcLikeChains = (btcItems + moneroItems + zanoItems + zcashItems).sortedBy { it.blockchainItem.blockchain.type.order }

            otherChains = blockchainItems
                .filterNot { it is BlockchainSettingsModule.BlockchainItem.Btc }
                .mapNotNull { item ->
                    when (item) {
                        is BlockchainSettingsModule.BlockchainItem.Evm -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.syncSource.name,
                            imageUrl = item.blockchain.type.imageUrl,
                            blockchainItem = item
                        )
                        is BlockchainSettingsModule.BlockchainItem.Solana -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.rpcSource.name,
                            imageUrl = item.blockchain.type.imageUrl,
                            blockchainItem = item
                        )
                        else -> null
                    }
                }
        }
    }

}
