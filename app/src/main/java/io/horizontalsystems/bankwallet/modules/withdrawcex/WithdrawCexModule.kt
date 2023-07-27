package cash.p.terminal.modules.withdrawcex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.providers.CexAsset
import cash.p.terminal.core.providers.CexWithdrawNetwork
import cash.p.terminal.core.providers.CoinzixCexProvider
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.xrate.XRateService

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
