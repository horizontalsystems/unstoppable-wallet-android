package cash.p.terminal.modules.send.solana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.core.adapters.SolanaAdapter
import cash.p.terminal.core.isNative
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import io.horizontalsystems.core.entities.BlockchainType
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import io.horizontalsystems.solanakit.SolanaKit
import java.math.RoundingMode

object SendSolanaModule {

    class Factory(
        private val wallet: cash.p.terminal.wallet.Wallet,
        private val predefinedAddress: String?,
    ) : ViewModelProvider.Factory {
        val adapter = (App.adapterManager.getAdapterForWallet(wallet) as? ISendSolanaAdapter) ?: throw IllegalStateException("SendSolanaAdapter is null")

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendSolanaViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        adapter.availableBalance.setScale(coinMaxAllowedDecimals, RoundingMode.DOWN),
                        wallet.token.type.isNative,
                    )
                    val solToken = App.coinManager.getToken(TokenQuery(BlockchainType.Solana, TokenType.Native)) ?: throw IllegalArgumentException()
                    val balance = App.solanaKitManager.solanaKitWrapper?.solanaKit?.balance ?: 0L
                    val solBalance = SolanaAdapter.balanceInBigDecimal(balance, solToken.decimals) - SolanaKit.accountRentAmount
                    val addressService = SendSolanaAddressService(predefinedAddress)
                    val xRateService = XRateService(App.marketKit, App.currencyManager.baseCurrency)

                    SendSolanaViewModel(
                        wallet = wallet,
                        sendToken = wallet.token,
                        feeToken = solToken,
                        solBalance = solBalance,
                        adapter = adapter,
                        xRateService = xRateService,
                        amountService = amountService,
                        addressService = addressService,
                        coinMaxAllowedDecimals = coinMaxAllowedDecimals,
                        contactsRepo = App.contactsRepository,
                        showAddressInput = predefinedAddress == null,
                        connectivityManager = App.connectivityManager,
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

}