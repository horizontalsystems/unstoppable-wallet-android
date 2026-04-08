package cash.p.terminal.modules.multiswap.sendtransaction.services

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.ISendStellarAdapter
import cash.p.terminal.core.managers.StellarKitManager
import cash.p.terminal.entities.CoinValue
import cash.p.terminal.modules.multiswap.sendtransaction.ISendTransactionService
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionData
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionResult
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionServiceState
import cash.p.terminal.modules.multiswap.sendtransaction.SendTransactionSettings
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.Token
import io.horizontalsystems.stellarkit.StellarKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.java.KoinJavaComponent.inject
import java.math.BigDecimal

class SendTransactionServiceStellar(account: Account, token: Token) :
    ISendTransactionService<ISendStellarAdapter>(token) {
    private val stellarKitManager: StellarKitManager by inject(StellarKitManager::class.java)
    private val stellarKit: StellarKit = runBlocking { stellarKitManager.getStellarKitWrapper(account) }.stellarKit

    private var fee: BigDecimal? = stellarKit.sendFee

    private var transactionEnvelope: String? = null

    private var recipient: String? = null
    private var memo: String? = null
    private var amount: BigDecimal? = null

    private val _sendTransactionSettingsFlow = MutableStateFlow(
        SendTransactionSettings.Common
    )
    override val sendTransactionSettingsFlow: StateFlow<SendTransactionSettings> =
        _sendTransactionSettingsFlow.asStateFlow()

    override fun start(coroutineScope: CoroutineScope) = Unit

    override suspend fun setSendTransactionData(data: SendTransactionData) {
        check(data is SendTransactionData.Stellar)

        when (data) {
            is SendTransactionData.Stellar.Regular -> {
                recipient = data.address
                memo = data.memo
                amount = data.amount
            }

            is SendTransactionData.Stellar.WithTransactionEnvelope -> {
                transactionEnvelope = data.transactionEnvelope
                fee = StellarKit.estimateFee(data.transactionEnvelope)
            }
        }

        emitState()
    }

    override fun hasSettings() = false

    @Composable
    override fun GetSettingsContent(navController: NavController) = Unit

    override suspend fun sendTransaction(mevProtectionEnabled: Boolean): SendTransactionResult {
        val response = if (transactionEnvelope != null) {
            val transactionEnvelope = requireNotNull(transactionEnvelope)
            stellarKit.sendTransaction(transactionEnvelope)
        } else if (recipient != null && amount != null && memo != null) {
            val recipient = requireNotNull(recipient)
            val amount = requireNotNull(amount)
            stellarKit.sendNative(recipient, amount, memo)
        } else {
            throw IllegalStateException("Transaction data not set")
        }
        return SendTransactionResult.Stellar(response)
    }

    override fun createState(): SendTransactionServiceState {
        val adjustedBalance = adapterManager.getAdjustedBalanceData(wallet)?.available
        val maxSendable = adapter.maxSpendableBalance
        // Use minOf to ensure we don't show more than maxSpendableBalance (which accounts for fee)
        val availableBalance = if (adjustedBalance != null) {
            minOf(adjustedBalance, maxSendable)
        } else {
            maxSendable
        }
        return SendTransactionServiceState(
            availableBalance = availableBalance,
            networkFee = fee?.let {
                getAmountData(CoinValue(feeToken, it))
            },
            cautions = listOf(),
            sendable = transactionEnvelope != null,
            loading = false,
            fields = listOf(),
            extraFees = extraFees
        )
    }
}
