package cash.p.terminal.modules.send.bitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendBitcoinAdapter
import cash.p.terminal.core.factories.FeeRateProviderFactory
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.xrate.XRateService

object SendBitcoinModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val adapter = App.adapterManager.getAdapterForWallet(wallet) as ISendBitcoinAdapter

            val provider = FeeRateProviderFactory.provider(wallet.token.blockchainType)!!
            val feeService = SendBitcoinFeeService(adapter)
            val feeRateService = SendBitcoinFeeRateService(provider)
            val amountService = SendBitcoinAmountService(adapter, wallet.coin.code, AmountValidator())
            val addressService = SendBitcoinAddressService(adapter)
            val pluginService = SendBitcoinPluginService(App.localStorage, wallet.token.blockchainType)
            return SendBitcoinViewModel(
                adapter,
                wallet,
                feeRateService,
                feeService,
                amountService,
                addressService,
                pluginService,
                XRateService(App.marketKit, App.currencyManager.baseCurrency),
                App.btcBlockchainManager
            )  as T
        }
    }
}
