package io.horizontalsystems.walletkit.modules.crosspay

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IBalanceAdapter
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.core.badge
import io.horizontalsystems.walletkit.core.ethereum.CautionViewItem
import io.horizontalsystems.walletkit.core.managers.CurrencyManager
import io.horizontalsystems.walletkit.entities.CoinValue
import io.horizontalsystems.walletkit.entities.Currency
import io.horizontalsystems.walletkit.entities.SwapRecord
import io.horizontalsystems.walletkit.modules.multiswap.TimerService
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapRecordManager
import io.horizontalsystems.walletkit.modules.multiswap.history.SwapStatus
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionResult
import io.horizontalsystems.walletkit.modules.multiswap.sendtransaction.SendTransactionServiceFactory
import io.horizontalsystems.walletkit.modules.multiswap.ui.DataField
import io.horizontalsystems.walletkit.modules.send.SendModule
import io.horizontalsystems.walletkit.modules.xrate.XRateService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

/**
 * The confirmation step of a CrossPay payment: commits the exact-output order, then builds
 * and estimates the deposit transfer through the source chain's own send-transaction
 * service. This ViewModel never builds a transaction itself.
 *
 * Committing happens once per user-visible order, and that is mandatory rather than an
 * optimisation: every /v2/swap creates a REAL order. Fee-settings changes re-estimate the
 * same deposit; they never re-commit. Past [QUOTE_LIFETIME_SECONDS] the send button is
 * replaced by Refresh — an order is only ever replaced by that explicit tap, never silently
 * under a live send button. The expired order was never funded and simply lapses server-side.
 */
class CrossPayConfirmViewModel(
    private val request: CrossPayRequest,
    private val manager: CrossPayManager,
    val sendTransactionService: AbstractSendTransactionService,
    currencyManager: CurrencyManager,
    private val timerService: TimerService,
    private val swapRecordManager: SwapRecordManager,
) : ViewModelUiState<CrossPayConfirmUiState>() {

    private val tokenIn = request.tokenIn
    private val tokenOut = request.tokenOut
    private val currency = currencyManager.baseCurrency

    // Same sources the per-chain confirmations use for the shared top section.
    // rateOut prices what the recipient gets; rateIn prices the deposit rows.
    val rateOut = XRateService(App.marketKit, currency).getRate(tokenOut.coin.uid)
    val rateIn = XRateService(App.marketKit, currency).getRate(tokenIn.coin.uid)
    val contact = App.contactsRepository
        .getContactsFiltered(tokenOut.blockchainType, addressQuery = request.recipient)
        .firstOrNull()

    private var order: CrossPayOrder? = null
    private var depositData: SendTransactionData? = null
    private var error: Throwable? = null
    private var initialLoading = true
    private var expired = false
    private var sendTransactionState = sendTransactionService.stateFlow.value
    private var timerState = timerService.stateFlow.value

    // Reject a second concurrent broadcast (double-tap / overlapping send) so the same
    // deposit is never sent twice.
    private var isSending = false

    // The one history row for this order. A retry after a failed broadcast reuses it
    // instead of inserting a duplicate per attempt.
    private var recordId: Int? = null

    // The in-flight commit. A new commit cancels the previous one: without this a double
    // tap on Refresh starts two commits whose `order`/`depositData` writes interleave —
    // the deposit could fund one order while the screen and history describe the other.
    private var commitJob: Job? = null

    init {
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
            // its sellAmount ceiling are never silently replaced. Guarded in the body (a
            // downstream throw would kill viewModelScope and freeze the screen; Flow.catch
            // only sees upstream failures) so the collector survives for later changes.
            sendTransactionService.sendTransactionSettingsFlow.collect {
                val data = depositData ?: return@collect
                try {
                    sendTransactionService.setSendTransactionData(data)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    error = e
                    emitState()
                }
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

    override fun createState(): CrossPayConfirmUiState {
        val order = order

        return CrossPayConfirmUiState(
            initialLoading = initialLoading,
            loading = sendTransactionState.loading,
            tokenIn = tokenIn,
            tokenOut = tokenOut,
            recipient = request.recipient,
            amountOut = order?.amountOut ?: request.amountOut,
            depositAmount = order?.depositAmount,
            reservedAmount = order?.refundableBuffer?.takeIf { it > BigDecimal.ZERO },
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
        // Replaces the service's own rejection instead of adding to it: the service names no
        // amount, and under exact output the figure the user needs is the DEPOSIT plus its
        // network fee, not what they entered — without this the refusal is baffling. Two
        // messages for one shortfall read as two problems.
        order?.let { order ->
            if (insufficientBalance(order)) {
                return listOf(
                    CautionViewItem(
                        title = App.instance.getString(R.string.CrossPay_Caution_Title),
                        text = App.instance.getString(
                            R.string.CrossPay_Caution_InsufficientBalance,
                            CoinValue(tokenIn, order.depositAmount).getFormattedFull(),
                        ),
                        type = CautionViewItem.Type.Error,
                    )
                )
            }
        }

        return sendTransactionState.cautions
    }

    private fun insufficientBalance(order: CrossPayOrder): Boolean {
        val available = App.adapterManager
            .getAdapterForToken<IBalanceAdapter>(tokenIn)
            ?.balanceData?.available
            ?: return false

        return order.depositAmount > available
    }

    /**
     * The expired-state action: commits a brand-new order in place. The screen returns to
     * the exact just-opened state — old order, deposit data and history-row binding are
     * dropped first, so a stale order can never sit under a live send button and the new
     * order gets its own history row (a Failed row from the old order's broadcast stays).
     */
    fun refresh() {
        synchronized(this) {
            if (isSending) return
        }

        expired = false
        order = null
        depositData = null
        recordId = null
        initialLoading = true
        emitState()

        commit()
    }

    private fun commit() {
        commitJob?.cancel()
        commitJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                error = null

                val order = manager.commit(request)

                ensureActive()

                this@CrossPayConfirmViewModel.order = order
                emitState()

                val provider = manager.provider ?: throw CrossPayError.CommitFailed()
                val data = provider.depositTransactionData(tokenIn, order.depositAmount, order.route)
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
        val order = order ?: throw CrossPayError.CommitFailed()

        synchronized(this@CrossPayConfirmViewModel) {
            if (isSending) throw CrossPayError.CommitFailed()
            isSending = true
        }

        try {
            timerService.stop()

            // Pre-saved before broadcasting so the record survives the app dying between the
            // two: the committed order is already real server-side, and /v2/track resolves it
            // by uuid alone even before a tx hash lands. A retry reuses the row (flipping a
            // previous Failed back to Depositing) rather than inserting one per attempt.
            val savedRecordId = recordId?.also {
                swapRecordManager.updateStatus(it, SwapStatus.Depositing, null)
            } ?: swapRecordManager.save(swapRecord(order)).also { recordId = it }

            val result = try {
                sendTransactionService.sendTransaction()
            } catch (e: Throwable) {
                // A failed broadcast moved no funds; the unfunded order simply expires.
                swapRecordManager.updateStatus(savedRecordId, SwapStatus.Failed, null)

                // Stopping the timer above silenced the expiry watchdog; a retry is only
                // legitimate while the committed quote is still alive, so rearm it with the
                // remaining lifetime — or dead-end the screen if the deadline already passed.
                val remainingSeconds =
                    (order.committedAt + QUOTE_LIFETIME_SECONDS * 1000 - System.currentTimeMillis()) / 1000
                withContext(Dispatchers.Main) {
                    if (remainingSeconds > 0) {
                        timerService.start(remainingSeconds)
                    } else {
                        expired = true
                        emitState()
                    }
                }

                throw e
            }

            transactionHash(result)?.let {
                swapRecordManager.updateTransactionHash(savedRecordId, it)
            }

            // The recipient is a wallet-external address worth suggesting next time —
            // on the DESTINATION chain, where they receive.
            App.recentAddressManager.setRecentAddress(
                io.horizontalsystems.walletkit.entities.Address(request.recipient),
                tokenOut.blockchainType,
            )

            result
        } finally {
            synchronized(this@CrossPayConfirmViewModel) { isSending = false }
        }
    }

    private fun swapRecord(order: CrossPayOrder) = SwapRecord(
        accountId = App.accountManager.activeAccount?.id ?: "",
        timestamp = System.currentTimeMillis(),
        providerId = order.providerId,
        providerName = order.providerName,
        tokenInUid = tokenIn.tokenQuery.id,
        tokenInCoinCode = tokenIn.coin.code,
        tokenInCoinUid = tokenIn.coin.uid,
        tokenInBadge = tokenIn.badge,
        tokenInBlockchainTypeUid = tokenIn.blockchainType.uid,
        tokenOutUid = tokenOut.tokenQuery.id,
        tokenOutCoinCode = tokenOut.coin.code,
        tokenOutCoinUid = tokenOut.coin.uid,
        tokenOutBadge = tokenOut.badge,
        tokenOutBlockchainTypeUid = tokenOut.blockchainType.uid,
        amountIn = order.depositAmount.toPlainString(),
        amountOut = order.amountOut.toPlainString(),
        amountOutMin = order.amountOut.toPlainString(),
        recipientAddress = order.request.recipient,
        customRecipientAddress = true,
        // Doubles as the refund address — the buffer refund lands here.
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
        const val QUOTE_LIFETIME_SECONDS = 15L

        fun init(
            request: CrossPayRequest,
        ): CreationExtras.() -> CrossPayConfirmViewModel = {
            CrossPayConfirmViewModel(
                request = request,
                manager = CrossPayManager(),
                sendTransactionService = SendTransactionServiceFactory.create(request.tokenIn),
                currencyManager = App.currencyManager,
                timerService = TimerService(),
                swapRecordManager = App.swapRecordManager,
            )
        }
    }
}

data class CrossPayConfirmUiState(
    val initialLoading: Boolean,
    val loading: Boolean,
    val tokenIn: io.horizontalsystems.marketkit.models.Token,
    val tokenOut: io.horizontalsystems.marketkit.models.Token,
    val recipient: String,
    val amountOut: BigDecimal,
    // What the sender transfers, in tokenIn. Null until the order is committed.
    val depositAmount: BigDecimal?,
    // The refundable slippage buffer in tokenIn — shown as "Reserved amount".
    val reservedAmount: BigDecimal?,
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
