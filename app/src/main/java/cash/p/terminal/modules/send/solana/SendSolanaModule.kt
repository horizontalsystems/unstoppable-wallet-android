package cash.p.terminal.modules.send.solana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.send.SendAmountAdvancedService
import cash.p.terminal.modules.xrate.XRateService
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.RoundingMode

object SendSolanaModule {

    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        val adapter by lazy {
            App.adapterManager.getAdapterForWallet(wallet) as ISendSolanaAdapter
        }

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
                    val feeToken = App.coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: throw IllegalArgumentException()

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