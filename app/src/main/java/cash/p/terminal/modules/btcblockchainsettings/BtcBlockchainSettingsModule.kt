package cash.p.terminal.modules.btcblockchainsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Blockchain

internal object BtcBlockchainSettingsModule {

    class Factory(private val blockchain: Blockchain) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = BtcBlockchainSettingsService(
                blockchain,
                App.btcBlockchainManager
            )

            return BtcBlockchainSettingsViewModel(service) as T
        }
    }

    data class ViewItem(
        val id: String,
        val title: String,
        val subtitle: String,
        val selected: Boolean,
        val icon: BlockchainSettingsIcon
    )

    sealed class BlockchainSettingsIcon {
        data class ApiIcon(val resId: Int): BlockchainSettingsIcon()
        data class BlockchainIcon(val url: String): BlockchainSettingsIcon()
    }
}