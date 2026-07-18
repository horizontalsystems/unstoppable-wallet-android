package io.horizontalsystems.walletkit.modules.send.monero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendMoneroAdapter
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

object SendMoneroModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ISendMoneroAdapter>(wallet) ?: throw IllegalStateException("ISendMoneroAdapter is null")
        val balanceAdapter = App.adapterManager.getBalanceAdapterForWallet(wallet) ?: throw IllegalStateException("IBalanceAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val amountService = SendAmountService(
                amountValidator = amountValidator,
                coinCode = wallet.coin.code,
                availableBalance = adapter.balanceData.available
            )
            val addressService = SendMoneroAddressService()
            val feeService = SendMoneroFeeService(adapter, App.connectivityManager)
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Monero, TokenType.Native)) ?: throw IllegalArgumentException()

            return SendMoneroViewModel(
                wallet,
                wallet.token,
                feeToken,
                adapter,
                coinMaxAllowedDecimals,
                xRateService,
                address,
                !hideAddress,
                amountService,
                addressService,
                feeService,
                App.contactsRepository,
                App.recentAddressManager,
                balanceAdapter,
                App.localStorage
            ) as T
        }
    }
}
