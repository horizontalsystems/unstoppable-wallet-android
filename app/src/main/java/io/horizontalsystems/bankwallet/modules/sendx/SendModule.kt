package io.horizontalsystems.bankwallet.modules.sendx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendBitcoinAdapter
import io.horizontalsystems.bankwallet.entities.Wallet

object SendModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendBitcoinAdapter
            val service = SendBitcoinService(adapter, App.localStorage, wallet.coinType)

            return SendViewModel(service) as T
        }
    }

}


