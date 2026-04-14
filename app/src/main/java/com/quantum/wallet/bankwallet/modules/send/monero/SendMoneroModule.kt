package com.quantum.wallet.bankwallet.modules.send.monero

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ISendMoneroAdapter
import com.quantum.wallet.bankwallet.core.isNative
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.AmountValidator
import com.quantum.wallet.bankwallet.modules.amount.SendAmountService
import com.quantum.wallet.bankwallet.modules.xrate.XRateService
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
            val feeService = SendMoneroFeeService(adapter)
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
                App.recentAddressManager
            ) as T
        }
    }
}
