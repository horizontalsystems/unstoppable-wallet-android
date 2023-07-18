package cash.p.terminal.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexWithdrawNetwork
import cash.p.terminal.core.providers.ICexProvider
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.xrate.XRateService

object WithdrawCexModule {
    class Factory(
        private val cexAsset: CexAsset,
        private val network: CexWithdrawNetwork,
        private val cexProvider: ICexProvider
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
