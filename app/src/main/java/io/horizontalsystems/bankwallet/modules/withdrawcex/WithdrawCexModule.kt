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
            val amountService = CexWithdrawAmountService(amountValidator, cexAsset.id, cexAsset.freeBalance, network, cexAsset.decimals)

            return WithdrawCexViewModel(
                cexAsset,
                network,
                XRateService(App.marketKit, App.currencyManager.baseCurrency),
                amountService,
                CexWithdrawAddressService(),
                cexProvider,
                App.contactsRepository
            ) as T
        }
    }

    sealed class CodeGetButtonState {
        object Active : CodeGetButtonState()
        class Pending(val secondsLeft: Int) : CodeGetButtonState()
    }

}
