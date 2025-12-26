package io.horizontalsystems.bankwallet.modules.send.stellar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendStellarAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
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