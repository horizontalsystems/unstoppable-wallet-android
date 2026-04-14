package com.quantum.wallet.bankwallet.modules.send.zcash.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.adapters.zcash.ZcashAdapter
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.xrate.XRateService

object ShieldZcashModule {

    class Factory(
        private val wallet: Wallet,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ZcashAdapter>(wallet) ?: throw IllegalStateException("ZcashAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return ShieldZcashViewModel(
                adapter,
                wallet,
                xRateService
            ) as T
        }
    }

}
