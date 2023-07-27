package io.horizontalsystems.bankwallet.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.core.providers.CexWithdrawNetwork
import io.horizontalsystems.bankwallet.core.providers.CoinzixCexProvider
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

object WithdrawCexModule {
    class Factory(
        private val cexAsset: CexAsset,
        private val network: CexWithdrawNetwork,
        private val cexProvider: CoinzixCexProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val amountService = CexWithdrawAmountService(
                amountValidator = amountValidator,
                coinCode = cexAsset.id,
                freeBalance = cexAsset.freeBalance,
                network = network,
                decimals = cexAsset.decimals,
                numberFormatter = App.numberFormatter
            )

            return WithdrawCexViewModel(
                cexAsset = cexAsset,
                network = network,
                xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency),
                amountService = amountService,
                addressService = CexWithdrawAddressService(network.blockchain),
                cexProvider = cexProvider,
                contactsRepository = App.contactsRepository
            ) as T
        }
    }
}
