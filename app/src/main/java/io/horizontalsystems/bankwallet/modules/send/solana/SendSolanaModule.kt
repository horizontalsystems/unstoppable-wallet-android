package io.horizontalsystems.bankwallet.modules.send.solana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.amount.AmountValidator
import io.horizontalsystems.bankwallet.modules.send.SendAmountAdvancedService
import io.horizontalsystems.bankwallet.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.Token
import java.math.RoundingMode

object SendSolanaModule {

    class Factory(
        private val wallet: Wallet,
        private val adapter: ISendSolanaAdapter,
        private val feeToken: Token
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendSolanaViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountAdvancedService(
                        adapter.availableBalance.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token,
                        amountValidator,
                    )
                    val addressService = SendSolanaAddressService()
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendSolanaViewModel(
                        wallet,
                        wallet.token,
                        feeToken,
                        adapter,
                        xRateService,
                        amountService,
                        addressService,
                        coinMaxAllowedDecimals,
                        App.contactsRepository
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

}