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
import io.horizontalsystems.walletkit.modules.multiswap.TimerService
import io.horizontalsystems.walletkit.modules.xrate.XRateService
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
import kotlinx.coroutines.Job
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
 * Committing happens once per user-visible order, and that is mandatory rather than an
 * optimisation: every /v2/swap creates a REAL order. Fee-settings changes re-estimate the
 * same deposit; they never re-commit. Past [QUOTE_LIFETIME_SECONDS] the send button is
 * replaced by Refresh — an order is only ever replaced by that explicit tap, never silently
 * under a live send button. The expired order was never funded and simply lapses server-side.
 */
class PrivateSendConfirmViewModel(
    private val request: PrivateSendRequest,
    private val btcParams: PrivateSendBtcParams?,
    private val manager: PrivateSendManager,
    val sendTransactionService: AbstractSendTransactionService,
    currencyManager: CurrencyManager,
    private val timerService: TimerService,
    private val swapRecordManager: SwapRecordManager,
) : ViewModelUiState<PrivateSendConfirmUiState>() {

    private val token = request.token
    private val currency = currencyManager.baseCurrency

    // Same sources the per-chain confirmations use for the shared top section.
    val coinRate = XRateService(App.marketKit, currency).getRate(token.coin.uid)
    val contact = App.contactsRepository
        .getContactsFiltered(token.blockchainType, addressQuery = request.recipient)
        .firstOrNull()

    private var order: PrivateSendOrder? = null
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

    override fun createState(): PrivateSendConfirmUiState {
        val order = order

        return PrivateSendConfirmUiState(
            initialLoading = initialLoading,
            loading = sendTransactionState.loading,
            token = token,
            recipient = request.recipient,
            amountOut = order?.amountOut ?: request.amountOut,
            privateFee = order?.privateFee,
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

                this@PrivateSendConfirmViewModel.order = order
                emitState()

                val data = PrivateSendDepositBuilder.build(order, btcParams)
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
        const val QUOTE_LIFETIME_SECONDS = 15L

        fun init(
            request: PrivateSendRequest,
            btcParams: PrivateSendBtcParams?,
        ): CreationExtras.() -> PrivateSendConfirmViewModel = {
            PrivateSendConfirmViewModel(
                request = request,
                btcParams = btcParams,
                manager = App.privateSendManager,
                sendTransactionService = SendTransactionServiceFactory.create(request.token),
                currencyManager = App.currencyManager,
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
    val privateFee: BigDecimal?,
    // The refundable slippage buffer — shown as "Reserved amount", per the design.
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
