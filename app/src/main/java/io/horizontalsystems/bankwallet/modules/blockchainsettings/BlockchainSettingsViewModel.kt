package io.horizontalsystems.bankwallet.modules.blockchainsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class BlockchainSettingsViewModel(
    private val service: BlockchainSettingsService
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var btcLikeChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    var otherChains by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
        private set

    init {
        service.blockchainItemsObservable
            .subscribeIO {
                sync(it)
            }.let {
                disposables.add(it)
            }

        service.start()
        sync(service.blockchainItems)
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    private fun sync(blockchainItems: List<BlockchainSettingsModule.BlockchainItem>) {
        viewModelScope.launch {
            btcLikeChains = blockchainItems
                .filterIsInstance<BlockchainSettingsModule.BlockchainItem.Btc>()
                .map { item ->
                    BlockchainSettingsModule.BlockchainViewItem(
                        title = item.blockchain.name,
                        subtitle = Translator.getString(item.restoreMode.title),
                        imageUrl = item.blockchain.type.imageUrl,
                        blockchainItem = item
                    )
                }

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
