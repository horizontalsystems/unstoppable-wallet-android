package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.core.ethereum.CautionViewItem
import io.horizontalsystems.bankwallet.modules.multiswap.providers.IMultiSwapProvider
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.AbstractSendTransactionService
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.FeeType
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import io.horizontalsystems.bankwallet.modules.send.SendModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class SwapFinalQuoteService(
    swapQuote: ISwapQuote,
    private val sendTransactionService: AbstractSendTransactionService,
    private val swapProvider: IMultiSwapProvider,
    private val swapSettings: Map<String, Any?>
) : ServiceState<SwapFinalQuoteService.State>() {
    private var sendTransactionSettings: SendTransactionSettings? = null
    private val tokenIn = swapQuote.tokenIn
    private val tokenOut = swapQuote.tokenOut
    private val amountIn = swapQuote.amountIn

    private var mevProtectionEnabled = false
    private var loading = true
    private var sendTransactionState = sendTransactionService.stateFlow.value

    private var amountOut: BigDecimal? = null
    private var amountOutMin: BigDecimal? = null
    private var priceImpact: BigDecimal? = null
    private var quoteFields: List<DataField> = listOf()
    private var cautionViewItems: List<CautionViewItem> = listOf()
    private var fetchFinalQuoteJob: Job? = null
    private val mevProtectionAvailable = sendTransactionService.mevProtectionAvailable

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {
        coroutineScope.launch {
            sendTransactionService.sendTransactionSettingsFlow.collect {
                sendTransactionSettings = it

                fetchFinalQuote()
            }
        }

        coroutineScope.launch {
            sendTransactionService.stateFlow.collect { transactionState ->
                sendTransactionState = transactionState

                loading = transactionState.loading

                emitState()

//                if (sendTransactionState.sendable) {
//                    timerService.start(10)
//                }
            }
        }


        sendTransactionService.start(coroutineScope)

        fetchFinalQuote()
    }

    fun stop() {
        coroutineScope.cancel()
    }

    override fun createState(): State {
        var cautions = sendTransactionState.cautions

        if (cautions.isEmpty()) {
            cautions += cautionViewItems
        }

        return State(
            loading = loading,
            amountOut = amountOut,
            amountOutMin = amountOutMin,
            networkFee = sendTransactionState.networkFee,
            extraFees = sendTransactionState.extraFees,
            cautions = cautions,
            validQuote = sendTransactionState.sendable,
            quoteFields = quoteFields,
            transactionFields = sendTransactionState.fields,
            hasSettings = sendTransactionService.hasSettings,
            mevProtectionAvailable = mevProtectionAvailable,
            mevProtectionEnabled = mevProtectionEnabled,
            priceImpact = priceImpact
        )
    }

    fun refresh() {
        loading = true
        emitState()

        sendTransactionService.refreshUuid()
        fetchFinalQuote()

//        stat(priceImpactLevelage = StatPage.SwapConfirmation, event = StatEvent.Refresh)
    }

    private fun fetchFinalQuote() {
        fetchFinalQuoteJob?.cancel()
        fetchFinalQuoteJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                val finalQuote = swapProvider.fetchFinalQuote(tokenIn, tokenOut, amountIn, swapSettings, sendTransactionSettings)

                ensureActive()

                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                quoteFields = finalQuote.fields
                cautionViewItems = finalQuote.cautions.map(HSCaution::toCautionViewItem)
                priceImpact = finalQuote.priceImpact
                emitState()

                sendTransactionService.setSendTransactionData(finalQuote.sendTransactionData)
            } catch (e: CancellationException) {
                // Do nothing
            } catch (t: Throwable) {
//                Log.e("AAA", "fetchFinalQuote error", t)
            }
        }
    }

    suspend fun swap() = withContext(Dispatchers.Default) {
//        stat(page = StatPage.SwapConfirmation, event = StatEvent.Send)

        sendTransactionService.sendTransaction(mevProtectionEnabled)
    }

    fun toggleMevProtection(enabled: Boolean) {
        mevProtectionEnabled = enabled

        emitState()
    }

    data class State(
        val loading: Boolean,
        val amountOut: BigDecimal?,
        val amountOutMin: BigDecimal?,
        val networkFee: SendModule.AmountData?,
        val extraFees: Map<FeeType, SendModule.AmountData>,
        val cautions: List<CautionViewItem>,
        val validQuote: Boolean,
        val quoteFields: List<DataField>,
        val transactionFields: List<DataField>,
        val hasSettings: Boolean,
        val mevProtectionAvailable: Boolean,
        val mevProtectionEnabled: Boolean,
        val priceImpact: BigDecimal?
    )
}
