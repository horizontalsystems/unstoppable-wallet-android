package io.horizontalsystems.bankwallet.modules.settings.security.blockchains

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.reactivex.disposables.CompositeDisposable

class BlockchainSettingsViewModel(
    private val service: BlockchainSettingsService
) : ViewModel() {

    private var disposables: CompositeDisposable = CompositeDisposable()

    var viewItems by mutableStateOf<List<BlockchainSettingsModule.BlockchainViewItem>>(listOf())
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
        viewItems = blockchainItems.map { item ->
            when (item) {
                is BlockchainSettingsModule.BlockchainItem.Btc -> BlockchainSettingsModule.BlockchainViewItem(
                    title = item.blockchain.title,
                    subtitle = "${Translator.getString(item.restoreMode.title)}, ${
                        Translator.getString(item.transactionMode.title)
                    }",
                    icon = item.blockchain.icon24
                )
                is BlockchainSettingsModule.BlockchainItem.Evm -> BlockchainSettingsModule.BlockchainViewItem(
                    title = item.blockchain.name,
                    subtitle = item.syncSource.name,
                    icon = item.blockchain.icon24
                )
            }
        }
    }

}
