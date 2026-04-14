package com.quantum.wallet.bankwallet.modules.send.stellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ISendStellarAdapter
import com.quantum.wallet.bankwallet.core.isNative
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.amount.AmountValidator
import com.quantum.wallet.bankwallet.modules.amount.SendAmountService
import com.quantum.wallet.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType

object SendStellarModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ISendStellarAdapter>(wallet) ?: throw IllegalStateException("ISendStellarAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val amountService = SendAmountService(
                amountValidator = amountValidator,
                coinCode = wallet.coin.code,
                availableBalance = adapter.maxSendableBalance,
                leaveSomeBalanceForFee = wallet.token.type.isNative
            )
            val addressService = SendStellarAddressService()
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Stellar, TokenType.Native)) ?: throw IllegalArgumentException()

            return SendStellarViewModel(
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
                App.contactsRepository,
                App.recentAddressManager,
                SendStellarMinimumAmountService(adapter)
            ) as T
        }
    }
}