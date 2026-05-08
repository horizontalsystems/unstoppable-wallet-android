package io.horizontalsystems.bankwallet.modules.send.zano

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendZanoAdapter
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

object SendZanoModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ISendZanoAdapter>(wallet)
            ?: throw IllegalStateException("ISendZanoAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val amountService = SendAmountService(
                amountValidator = amountValidator,
                coinCode = wallet.coin.code,
                availableBalance = adapter.balanceData.available
            )
            val addressService = SendZanoAddressService()
            val feeService = SendZanoFeeService(adapter)
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(TokenQuery(wallet.token.blockchainType, TokenType.Native))
                ?: throw IllegalArgumentException()

            return SendZanoViewModel(
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
                App.recentAddressManager
            ) as T
        }
    }
}
