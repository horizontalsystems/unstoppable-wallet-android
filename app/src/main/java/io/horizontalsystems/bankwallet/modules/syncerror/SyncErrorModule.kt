package io.horizontalsystems.bankwallet.modules.syncerror

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.marketkit.models.Blockchain

object SyncErrorModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
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

    sealed class BlockchainWrapper {
        data class Bitcoin(val blockchain: Blockchain) : BlockchainWrapper()
        data class Evm(val blockchain: Blockchain) : BlockchainWrapper()
        object Monero : BlockchainWrapper()
    }
}
