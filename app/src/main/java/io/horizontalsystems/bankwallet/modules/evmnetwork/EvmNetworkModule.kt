package io.horizontalsystems.bankwallet.modules.evmnetwork

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.EvmBlockchain

object EvmNetworkModule {
    fun args(blockchain: EvmBlockchain, account: Account): Bundle {
        return bundleOf("blockchain" to blockchain, "account" to account)
    }

    class Factory(arguments: Bundle) : ViewModelProvider.Factory {
        private val blockchain = arguments.getParcelable<EvmBlockchain>("blockchain")!!
        private val account = arguments.getParcelable<Account>("account")!!

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val service = EvmNetworkService(
                blockchain,
                account,
                App.evmSyncSourceManager
            )

            return EvmNetworkViewModel(service) as T
        }
    }

}
