package cash.p.terminal.modules.syncerror

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Blockchain

object SyncErrorModule {

    class Factory(private val wallet: cash.p.terminal.wallet.Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = SyncErrorService(
                wallet,
                App.adapterManager,
                App.appConfigProvider.reportEmail,
                App.btcBlockchainManager,
                App.evmBlockchainManager
            )
            return SyncErrorViewModel(service) as T
        }
    }

    data class BlockchainWrapper(
        val blockchain: Blockchain,
        val type: Type
    ) {
        enum class Type {
            Bitcoin, Evm
        }
    }
}
