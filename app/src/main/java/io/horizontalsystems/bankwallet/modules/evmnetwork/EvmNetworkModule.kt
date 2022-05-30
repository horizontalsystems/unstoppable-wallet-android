package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.EvmBlockchain

object EvmNetworkModule {

    fun args(blockchain: EvmBlockchain): Bundle {
        return bundleOf("blockchain" to blockchain,)
    }

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val blockchain = arguments.getParcelable<EvmBlockchain>("blockchain")!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = EvmNetworkService(
                blockchain,
                App.evmSyncSourceManager
            )

            return EvmNetworkViewModel(service) as T
        }
    }

}
