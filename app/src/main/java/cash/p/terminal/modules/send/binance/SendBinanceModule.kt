package cash.p.terminal.modules.send.binance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBinanceAdapter
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService

object SendBinanceModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendBinanceAdapter
            val amountValidator = AmountValidator()
            val amountService = SendAmountService(amountValidator, wallet.coin.code, adapter.availableBalance)
            val addressService = SendBinanceAddressService(adapter)
            val feeService = SendBinanceFeeService(adapter, wallet.token, App.feeCoinProvider)
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

            return SendBinanceViewModel(
                wallet,
                adapter,
                amountService,
                addressService,
                feeService,
                xRateService,
                App.contactsRepository
            ) as T
        }

    }

}
