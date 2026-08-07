package io.horizontalsystems.walletkit.modules.blockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.imageUrl
import io.horizontalsystems.walletkit.core.order
import io.horizontalsystems.walletkit.core.providers.Translator
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

class BlockchainSettingsViewModel(
    private val service: BlockchainSettingsService
) : ViewModel() {

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
            val chainItems = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Chain>()
                .filter { it.btcLike }
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = item.subtitle,
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }
            btcLikeChains = chainItems.sortedBy { it.blockchainItem.blockchain.type.order }

            otherChains = blockchainItems
                .mapNotNull { item ->
                    when (item) {
                        is BlockchainSettingsModule.BlockchainItem.Evm -> BlockchainSettingsModule.BlockchainViewItem(
                            title = item.blockchain.name,
                            subtitle = item.syncSource.name,
                            imageUrl = item.blockchain.type.imageUrl,
                            blockchainItem = item
                        )
                        is BlockchainSettingsModule.BlockchainItem.Chain -> if (item.btcLike) null else {
                            BlockchainSettingsModule.BlockchainViewItem(
                                title = item.blockchain.name,
                                subtitle = item.subtitle,
                                imageUrl = item.blockchain.type.imageUrl,
                                blockchainItem = item
                            )
                        }

                        else -> null
                    }
                }
        }
    }

}
