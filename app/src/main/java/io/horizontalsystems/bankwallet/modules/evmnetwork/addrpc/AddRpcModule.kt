package io.horizontalsystems.bankwallet.modules.evmnetwork.addrpc

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.parcelable
import io.horizontalsystems.marketkit.models.Blockchain

object AddRpcModule {

    fun args(blockchain: Blockchain): Bundle {
        return bundleOf("blockchain" to blockchain)
    }

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val blockchain = arguments.parcelable<Blockchain>("blockchain")!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AddRpcViewModel(blockchain, App.evmSyncSourceManager) as T
        }
    }

}
