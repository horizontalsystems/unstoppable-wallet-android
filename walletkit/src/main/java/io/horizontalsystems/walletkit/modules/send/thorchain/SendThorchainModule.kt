package io.horizontalsystems.walletkit.modules.send.thorchain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendThorchainAdapter
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal

object SendThorchainModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ISendThorchainAdapter>(wallet) ?: throw IllegalStateException("ISendThorchainAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountValidator = AmountValidator()
            val coinMaxAllowedDecimals = wallet.token.decimals

            val availableBalance = if (adapter.isNativeCoin) {
                (adapter.availableBalance - adapter.fee).coerceAtLeast(BigDecimal.ZERO)
            } else {
                adapter.availableBalance
            }

            val amountService = SendAmountService(
                amountValidator = amountValidator,
                coinCode = wallet.coin.code,
                availableBalance = availableBalance,
                leaveSomeBalanceForFee = false
            )
            val addressService = SendThorchainAddressService()
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Thorchain, TokenType.Native)) ?: throw IllegalArgumentException()

            return SendThorchainViewModel(
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
                App.recentAddressManager
            ) as T
        }
    }
}
