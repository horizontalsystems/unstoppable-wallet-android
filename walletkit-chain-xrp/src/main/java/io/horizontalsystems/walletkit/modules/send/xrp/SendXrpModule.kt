package io.horizontalsystems.walletkit.modules.send.xrp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ISendXrpAdapter
import io.horizontalsystems.walletkit.core.isNative
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.amount.AmountValidator
import io.horizontalsystems.walletkit.modules.amount.SendAmountService
import io.horizontalsystems.walletkit.modules.xrate.XRateService

object SendXrpModule {
    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = App.adapterManager.getAdapterForWallet<ISendXrpAdapter>(wallet)
            ?: throw IllegalStateException("ISendXrpAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val amountService = SendAmountService(
                amountValidator = AmountValidator(),
                coinCode = wallet.coin.code,
                availableBalance = adapter.maxSendableBalance,
                leaveSomeBalanceForFee = wallet.token.type.isNative
            )
            val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
            val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Xrp, TokenType.Native))
                ?: throw IllegalArgumentException()

            return SendXrpViewModel(
                wallet = wallet,
                sendToken = wallet.token,
                feeToken = feeToken,
                adapter = adapter,
                coinMaxAllowedDecimals = wallet.token.decimals,
                xRateService = xRateService,
                address = address,
                showAddressInput = !hideAddress,
                amountService = amountService,
                addressService = SendXrpAddressService(),
                contactsRepo = App.contactsRepository,
                recentAddressManager = App.recentAddressManager,
                destinationService = SendXrpDestinationService(adapter),
            ) as T
        }
    }
}
