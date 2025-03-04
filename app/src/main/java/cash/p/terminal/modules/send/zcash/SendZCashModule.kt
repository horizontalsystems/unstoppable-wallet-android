package cash.p.terminal.modules.send.zcash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.Wallet

object SendZCashModule {

    class Factory(
        private val wallet: Wallet,
        private val predefinedAddress: String?,
    ) : ViewModelProvider.Factory {
        val adapter =
            (App.adapterManager.getAdapterForWallet(wallet) as? ISendZcashAdapter) ?: throw IllegalStateException("SendZcashAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val amountService = SendAmountService(
                amountValidator = AmountValidator(),
                coinCode = wallet.coin.code,
                availableBalance = adapter.availableBalance
            )
            val addressService = SendZCashAddressService(adapter, predefinedAddress)
            val memoService = SendZCashMemoService()

            return SendZCashViewModel(
                adapter = adapter,
                wallet = wallet,
                xRateService = xRateService,
                amountService = amountService,
                addressService = addressService,
                memoService = memoService,
                contactsRepo = App.contactsRepository,
                showAddressInput = predefinedAddress == null
            ) as T
        }
    }
}
