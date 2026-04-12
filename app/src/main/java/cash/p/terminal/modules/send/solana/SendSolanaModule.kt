package cash.p.terminal.modules.send.solana

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.core.HSCaution
import cash.p.terminal.core.ISendSolanaAdapter
import cash.p.terminal.core.isNative
import cash.p.terminal.core.managers.PendingTransactionRegistrar
import cash.p.terminal.entities.Address
import cash.p.terminal.modules.amount.AmountValidator
import cash.p.terminal.modules.amount.SendAmountService
import cash.p.terminal.modules.xrate.XRateService
import cash.p.terminal.wallet.IAdapterManager
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.getMaxSendableBalance
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.solanakit.SolanaKit
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal
import java.math.RoundingMode

object SendSolanaModule {

    class Factory(
        private val wallet: Wallet,
        private val address: Address?,
        private val hideAddress: Boolean,
        private val adapter: ISendSolanaAdapter
    ) : ViewModelProvider.Factory {
        private val adapterManager: IAdapterManager by inject(IAdapterManager::class.java)
        private val pendingRegistrar: PendingTransactionRegistrar by inject(PendingTransactionRegistrar::class.java)

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                SendSolanaViewModel::class.java -> {
                    val amountValidator = AmountValidator()
                    val coinMaxAllowedDecimals = wallet.token.decimals

                    val availableBalance = adapterManager.getMaxSendableBalance(wallet, adapter.maxSpendableBalance)
                    val amountService = SendAmountService(
                        amountValidator,
                        wallet.token.coin.code,
                        availableBalance.setScale(
                            coinMaxAllowedDecimals,
                            RoundingMode.DOWN
                        ),
                        wallet.token.type.isNative,
                    )
                    val solToken = App.coinManager.getToken(
                        TokenQuery(
                            BlockchainType.Solana,
                            TokenType.Native
                        )
                    ) ?: throw IllegalArgumentException()
                    val solBalance = (adapterManager.getAdjustedBalanceDataForToken(solToken)?.available
                        ?: BigDecimal.ZERO) - SolanaKit.accountRentAmount
                    val addressService = SendSolanaAddressService()
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
                        showAddressInput = !hideAddress,
                        address = address,
                        connectivityManager = App.connectivityManager,
                        pendingRegistrar = pendingRegistrar,
                        adapterManager = adapterManager
                    ) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    data class SendUiState(
        val availableBalance: BigDecimal,
        val amountCaution: HSCaution?,
        val addressError: Throwable?,
        val canBeSend: Boolean,
        val showAddressInput: Boolean,
        val address: Address?,
        val isPoisonAddress: Boolean = false,
        val riskAccepted: Boolean = false,
    )
}