package com.quantum.wallet.bankwallet.modules.send.bitcoin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ISendBitcoinAdapter
import com.quantum.wallet.bankwallet.core.factories.FeeRateProviderFactory
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.AmountValidator
import com.quantum.wallet.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType

object SendBitcoinModule {
    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter =
            App.adapterManager.getAdapterForWallet<ISendBitcoinAdapter>(wallet) ?: throw IllegalStateException("SendBitcoinAdapter is null")

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val provider = FeeRateProviderFactory.provider(wallet.token.blockchainType)!!
            val feeService = SendBitcoinFeeService(adapter)
            val feeRateService = SendBitcoinFeeRateService(provider)
            val amountService = SendBitcoinAmountService(adapter, wallet.coin.code, AmountValidator())
            val addressService = SendBitcoinAddressService(adapter)
            val pluginService = SendBitcoinPluginService(wallet.token.blockchainType)
            return SendBitcoinViewModel(
                adapter,
                wallet,
                feeRateService,
                feeService,
                amountService,
                addressService,
                pluginService,
                XRateService(App.marketKit, App.currencyManager.baseCurrency),
                App.btcBlockchainManager,
                App.contactsRepository,
                !hideAddress,
                App.localStorage,
                address,
                App.recentAddressManager
            ) as T
        }
    }

    data class UtxoData(
        val type: UtxoType? = null,
        val value: String = "0 / 0",
    )

    enum class UtxoType {
        Auto,
        Manual
    }

    val BlockchainType.rbfSupported: Boolean
        get() = when (this) {
            BlockchainType.Bitcoin,
            BlockchainType.Litecoin -> true
            else -> false
        }

}
