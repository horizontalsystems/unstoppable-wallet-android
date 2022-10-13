package io.horizontalsystems.bankwallet.modules.receive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.IReceiveAdapter
import io.horizontalsystems.bankwallet.entities.Wallet

object ReceiveModule {

    class Factory(private val wallet: Wallet, private val receiveAdapter: IReceiveAdapter) :
        ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReceiveViewModel(wallet, receiveAdapter) as T
        }
    }

    class NoReceiverAdapter : Error("No Receiver Adapter")
}
