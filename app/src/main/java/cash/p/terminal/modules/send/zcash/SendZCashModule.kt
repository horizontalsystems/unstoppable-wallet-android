package cash.p.terminal.modules.send.zcash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendZcashAdapter
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService

object SendZCashModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendZcashAdapter
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val amountService = SendAmountService(
                AmountValidator(),
                wallet.coin.code,
                adapter.availableBalance
            )
            val addressService = SendZCashAddressService(adapter)
            val memoService = SendZCashMemoService()

            return SendZCashViewModel(adapter, wallet, xRateService, amountService, addressService, memoService, App.contactsRepository) as T
        }
    }
}
