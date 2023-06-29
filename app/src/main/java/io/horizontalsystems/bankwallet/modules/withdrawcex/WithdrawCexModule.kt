package io.horizontalsystems.bankwallet.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.CexAsset
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.market.ImageSource
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

object WithdrawCexModule {
    class Factory(private val cexAsset: CexAsset) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val amountService = SendAmountService(amountValidator, cexAsset.id, cexAsset.freeBalance)

            return WithdrawCexViewModel(
                cexAsset,
                XRateService(App.marketKit, App.currencyManager.baseCurrency),
                amountService,
                SendAddressService()
            ) as T
        }
    }

    data class NetworkViewItem(
        val title: String,
        val imageSource: ImageSource,
        val selected: Boolean,
    )

    sealed class CodeGetButtonState{
        object Active : CodeGetButtonState()
        class Pending(val secondsLeft: Int) : CodeGetButtonState()
    }

}
