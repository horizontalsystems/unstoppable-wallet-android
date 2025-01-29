package io.horizontalsystems.bankwallet.modules.send.tron

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.amount.SendAmountService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.RoundingMode

object SendTronModule {

    class Factory(
        private val wallet: Wallet,
        private val address: Address,
        private val hideAddress: Boolean,
    ) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendTronAdapter) ?: throw IllegalStateException("SendTronAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendTronViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        adapter.balanceData.available.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token.type.isNative,
                    )
                    val addressService = SendTronAddressService(adapter, wallet.token)
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)
                    val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Tron, TokenType.Native)) ?: throw IllegalArgumentException()

                    SendTronViewModel(
                        wallet,
                        wallet.token,
                        feeToken,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals,
                        App.contactsRepository,
                        !hideAddress,
                        App.connectivityManager,
                        address,
                        App.recentAddressManager
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }
}
