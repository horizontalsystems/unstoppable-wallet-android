package cash.p.terminal.modules.evmnetwork.addrpc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Blockchain

object AddRpcModule {

    class Factory(private val blockchain: Blockchain) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddRpcViewModel(blockchain, App.evmSyncSourceManager) as T
        }
    }

}
