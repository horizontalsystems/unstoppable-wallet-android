package io.horizontalsystems.walletkit.modules.privatesend

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.FiatService
import io.horizontalsystems.walletkit.modules.multiswap.TimerService
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

/**
 * The confirmation step of a private send: commits the order (quote + /v2/swap in one), then
 * builds and estimates the deposit transfer through the chain's own send-transaction service.
 * This ViewModel never builds a transaction itself.
 *
 * Committing happens exactly once for the screen's lifetime, and that is mandatory rather
 * than an optimisation: every /v2/swap creates a REAL order. Fee-settings changes re-estimate
 * the same deposit; they never re-commit. Past [QUOTE_LIFETIME_SECONDS] the screen dead-ends
 * ("go back and re-enter") instead of silently swapping the order under the send button.
 */
class PrivateSendConfirmViewModel(
    private val request: PrivateSendRequest,
    private val manager: PrivateSendManager,
    val sendTransactionService: AbstractSendTransactionService,
    currencyManager: CurrencyManager,
    private val fiatServiceAmountOut: FiatService,
    private val fiatServiceFee: FiatService,
    private val fiatServiceDeposit: FiatService,
    private val timerService: TimerService,
    private val swapRecordManager: SwapRecordManager,
) : ViewModelUiState<PrivateSendConfirmUiState>() {

    private val token = request.token
    private val currency = currencyManager.baseCurrency

    private var order: PrivateSendOrder? = null
    private var depositData: SendTransactionData? = null
    private var error: Throwable? = null
    private var initialLoading = true
    private var expired = false
    private var fiatAmountOut: BigDecimal? = null
    private var fiatFee: BigDecimal? = null
    private var fiatDeposit: BigDecimal? = null
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var timerState = timerService.stateFlow.value

    // Reject a second concurrent broadcast (double-tap / overlapping send) so the same
    // deposit is never sent twice.
    private var isSending = false

    init {
        fiatServiceAmountOut.setCurrency(currency)
        fiatServiceAmountOut.setToken(token)
        fiatServiceAmountOut.setAmount(request.amountOut)

        fiatServiceFee.setCurrency(currency)
        fiatServiceFee.setToken(token)

        fiatServiceDeposit.setCurrency(currency)
        fiatServiceDeposit.setToken(token)

        viewModelScope.launch {
            fiatServiceAmountOut.stateFlow.collect {
                fiatAmountOut = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceFee.stateFlow.collect {
                fiatFee = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            fiatServiceDeposit.stateFlow.collect {
                fiatDeposit = it.fiatAmount
                emitState()
            }
        }

        viewModelScope.launch {
            sendTransactionService.stateFlow.collect {
                sendTransactionState = it
                if (order != null) {
                    initialLoading = initialLoading && it.loading
                }
                emitState()
            }
        }

        viewModelScope.launch {
            // A fee-settings change re-estimates the SAME deposit — the committed order and
            // its sellAmount ceiling are never silently replaced.
            sendTransactionService.sendTransactionSettingsFlow.collect {
                val data = depositData ?: return@collect
                sendTransactionService.setSendTransactionData(data)
            }
        }

        viewModelScope.launch {
            timerService.stateFlow.collect {
                timerState = it
                if (it.timeout) {
                    expired = true
                }
                emitState()
            }
        }

        sendTransactionService.start(viewModelScope)

        commit()
    }

    override fun createState(): PrivateSendConfirmUiState {
        val order = order

        return PrivateSendConfirmUiState(
            initialLoading = initialLoading,
            loading = sendTransactionState.loading,
            token = token,
            recipient = request.recipient,
            amountOut = order?.amountOut ?: request.amountOut,
            fiatAmountOut = fiatAmountOut,
            privateFee = order?.privateFee,
            fiatPrivateFee = fiatFee,
            depositAmount = order?.depositAmount,
            fiatDepositAmount = fiatDeposit,
            refundableBuffer = order?.refundableBuffer?.takeIf { it > BigDecimal.ZERO },
            bufferUnknown = order != null && order.minSellAmount == null,
            estimatedTime = order?.estimatedTime,
            currency = currency,
            networkFee = sendTransactionState.networkFee,
            cautions = cautions(),
            transactionFields = sendTransactionState.fields,
            canSend = order != null && sendTransactionState.sendable && !expired && error == null,
            expired = expired,
            hasSettings = sendTransactionService.hasSettings,
            hasNonceSettings = sendTransactionService.hasNonceSettings,
            error = error,
        )
    }

    private fun cautions(): List<CautionViewItem> {
        val cautions = sendTransactionState.cautions.toMutableList()

        // The service's own rejection names no amount, and under exact output the figure the
        // user needs is the DEPOSIT plus its network fee, not what they entered — without
        // this the refusal is baffling (MAX always fails here by design).
        order?.let { order ->
            if (insufficientBalance(order)) {
                cautions.add(
                    CautionViewItem(
                        title = App.instance.getString(io.horizontalsystems.walletkit.R.string.PrivateSend_Caution_Title),
                        text = App.instance.getString(
                            io.horizontalsystems.walletkit.R.string.PrivateSend_Caution_InsufficientBalance,
                            io.horizontalsystems.walletkit.entities.CoinValue(token, order.depositAmount).getFormattedFull(),
                        ),
                        type = CautionViewItem.Type.Error,
                    )
                )
            }
        }

        return cautions
    }

    private fun insufficientBalance(order: PrivateSendOrder): Boolean {
        val available = App.adapterManager
            .getAdapterForToken<IBalanceAdapter>(token)
            ?.balanceData?.available
            ?: return false

        return order.depositAmount > available
    }

    private fun commit() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                error = null

                val order = manager.commit(request)

                ensureActive()

                this@PrivateSendConfirmViewModel.order = order
                fiatServiceAmountOut.setAmount(order.amountOut)
                fiatServiceFee.setAmount(order.privateFee)
                fiatServiceDeposit.setAmount(order.depositAmount)
                emitState()

                val data = PrivateSendDepositBuilder.build(order)
                depositData = data
                sendTransactionService.setSendTransactionData(data)

                // CountDownTimer binds to the current thread's Looper, so the expiry timer
                // must start on Main — this coroutine runs on Default.
                withContext(Dispatchers.Main) {
                    timerService.start(QUOTE_LIFETIME_SECONDS)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                initialLoading = false
                error = e
                emitState()
            }
        }
    }

    override fun onCleared() {
        timerService.stop()
    }

    suspend fun send(): SendTransactionResult = withContext(Dispatchers.Default) {
        val order = order ?: throw PrivateSendError.CommitFailed()

        synchronized(this@PrivateSendConfirmViewModel) {
            if (isSending) throw PrivateSendError.CommitFailed()
            isSending = true
        }

        try {
            timerService.stop()

            // Pre-saved before broadcasting so the record survives the app dying between the
            // two: the committed order is already real server-side, and /v2/track resolves it
            // by uuid alone even before a tx hash lands.
            val recordId = swapRecordManager.save(swapRecord(order))

            val result = try {
                sendTransactionService.sendTransaction()
            } catch (e: Throwable) {
                // A failed broadcast moved no funds; the unfunded order simply expires.
                swapRecordManager.updateStatus(recordId, SwapStatus.Failed, null)
                throw e
            }

            transactionHash(result)?.let {
                swapRecordManager.updateTransactionHash(recordId, it)
            }

            // The recipient is a wallet-external address worth suggesting next time.
            App.recentAddressManager.setRecentAddress(
                io.horizontalsystems.walletkit.entities.Address(request.recipient),
                token.blockchainType,
            )

            result
        } finally {
            synchronized(this@PrivateSendConfirmViewModel) { isSending = false }
        }
    }

    private fun swapRecord(order: PrivateSendOrder) = SwapRecord(
        accountId = App.accountManager.activeAccount?.id ?: "",
        timestamp = System.currentTimeMillis(),
        providerId = order.providerId,
        providerName = "Private send",
        tokenInUid = token.tokenQuery.id,
        tokenInCoinCode = token.coin.code,
        tokenInCoinUid = token.coin.uid,
        tokenInBadge = token.badge,
        tokenInBlockchainTypeUid = token.blockchainType.uid,
        tokenOutUid = token.tokenQuery.id,
        tokenOutCoinCode = token.coin.code,
        tokenOutCoinUid = token.coin.uid,
        tokenOutBadge = token.badge,
        tokenOutBlockchainTypeUid = token.blockchainType.uid,
        amountIn = order.depositAmount.toPlainString(),
        amountOut = order.amountOut.toPlainString(),
        amountOutMin = order.amountOut.toPlainString(),
        recipientAddress = order.request.recipient,
        customRecipientAddress = true,
        // Doubles as the refund address — the buffer refund lands here. The provider itself
        // is never told the sender's address.
        sourceAddress = order.refundAddress,
        transactionHash = null,
        providerSwapId = order.providerSwapId,
        fromAsset = null,
        toAsset = null,
        depositAddress = order.depositAddress,
        status = SwapStatus.Depositing.name,
        estimatedTime = order.estimatedTime,
    )

    private fun transactionHash(result: SendTransactionResult): String? = when (result) {
        is SendTransactionResult.Evm -> result.transactionHash
        is SendTransactionResult.Btc -> result.transactionRecord?.transactionHash
        is SendTransactionResult.Zcash -> result.transactionHash
        is SendTransactionResult.Solana -> result.txHash
        is SendTransactionResult.Tron -> result.txHash
        is SendTransactionResult.Thorchain -> result.txHash
        is SendTransactionResult.Stellar -> result.txHash
        else -> null
    }

    companion object {
        // A committed quote is only honoured for this long. Past it the screen dead-ends
        // rather than pretending to be current — the deposit ceiling is what the send button
        // authorizes.
        const val QUOTE_LIFETIME_SECONDS = 60L

        fun init(request: PrivateSendRequest): CreationExtras.() -> PrivateSendConfirmViewModel = {
            PrivateSendConfirmViewModel(
                request = request,
                manager = App.privateSendManager,
                sendTransactionService = SendTransactionServiceFactory.create(request.token),
                currencyManager = App.currencyManager,
                fiatServiceAmountOut = FiatService(App.marketKit),
                fiatServiceFee = FiatService(App.marketKit),
                fiatServiceDeposit = FiatService(App.marketKit),
                timerService = TimerService(),
                swapRecordManager = App.swapRecordManager,
            )
        }
    }
}

data class PrivateSendConfirmUiState(
    val initialLoading: Boolean,
    val loading: Boolean,
    val token: Token,
    val recipient: String,
    val amountOut: BigDecimal,
    val fiatAmountOut: BigDecimal?,
    val privateFee: BigDecimal?,
    val fiatPrivateFee: BigDecimal?,
    val depositAmount: BigDecimal?,
    val fiatDepositAmount: BigDecimal?,
    val refundableBuffer: BigDecimal?,
    val bufferUnknown: Boolean,
    val estimatedTime: Long?,
    val currency: Currency,
    val networkFee: SendModule.AmountData?,
    val cautions: List<CautionViewItem>,
    val transactionFields: List<DataField>,
    val canSend: Boolean,
    val expired: Boolean,
    val hasSettings: Boolean,
    val hasNonceSettings: Boolean,
    val error: Throwable?,
)
