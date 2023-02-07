package cash.p.terminal.modules.evmnetwork

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.marketkit.models.Blockchain

object EvmNetworkModule {

    fun args(blockchain: Blockchain): Bundle {
        return bundleOf("blockchain" to blockchain)
    }

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val blockchain = arguments.getParcelable<Blockchain>("blockchain")!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EvmNetworkViewModel(blockchain, App.evmSyncSourceManager) as T
        }
    }

}
