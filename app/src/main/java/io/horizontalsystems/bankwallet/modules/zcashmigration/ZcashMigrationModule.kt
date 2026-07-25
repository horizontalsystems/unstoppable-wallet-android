package io.horizontalsystems.bankwallet.modules.zcashmigration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.adapters.zcash.ZcashAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

object ZcashMigrationModule {

    class Factory(
        private val wallet: Wallet,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ZcashAdapter>(wallet)
            ?: throw IllegalStateException("ZcashAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return ZcashMigrationViewModel(
                adapter,
                wallet,
                xRateService
            ) as T
        }
    }

}
