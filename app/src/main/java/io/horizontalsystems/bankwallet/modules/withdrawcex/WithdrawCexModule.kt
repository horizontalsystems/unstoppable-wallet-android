package cash.p.terminal.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService

object WithdrawCexModule {
    class Factory(private val cexAsset: CexAsset) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val amountService = SendAmountService(amountValidator, cexAsset.id, cexAsset.freeBalance)

            val cexProvider = App.cexProviderManager.cexProviderFlow.value
            return WithdrawCexViewModel(
                cexAsset,
                XRateService(App.marketKit, App.currencyManager.baseCurrency),
                amountService,
                SendAddressService(),
                cexProvider
            ) as T
        }
    }

    sealed class CodeGetButtonState{
        object Active : CodeGetButtonState()
        class Pending(val secondsLeft: Int) : CodeGetButtonState()
    }

}
