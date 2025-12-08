package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.HSCaution
import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionData
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import io.horizontalsystems.bankwallet.modules.multiswap.ui.DataField
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class SwapFinalQuoteService2(
) : ServiceState<SwapFinalQuoteService2.State>() {
    private var swapSettings: Map<String, Any?> = mapOf()
    private var quote: SwapProviderQuote? = null

    private var sendTransactionSettings: SendTransactionSettings? = null
    private var amountOut: BigDecimal? = null

    private var amountOutMin: BigDecimal? = null
    private var priceImpact: BigDecimal? = null
    private var fields: List<DataField> = listOf()
    private var cautions: List<HSCaution> = listOf()
    private var sendTransactionData: SendTransactionData? = null
    private var confirmInProgress = false
    private var fetchFinalQuoteJob: Job? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun createState() = State(
        amountOut = amountOut,
        amountOutMin = amountOutMin,
        cautions = cautions,
        quoteFields = fields,
        priceImpact = priceImpact,
        sendTransactionData = sendTransactionData,
        confirmInProgress = confirmInProgress
    )

    private fun fetchFinalQuote() {
        fetchFinalQuoteJob?.cancel()
        fetchFinalQuoteJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                val quote = quote ?: return@launch

                val finalQuote = quote.provider.fetchFinalQuote(quote.tokenIn, quote.tokenOut, quote.amountIn, swapSettings, sendTransactionSettings)

                ensureActive()

                amountOut = finalQuote.amountOut
                amountOutMin = finalQuote.amountOutMin
                fields = finalQuote.fields
                cautions = finalQuote.cautions
                priceImpact = finalQuote.priceImpact
                sendTransactionData = finalQuote.sendTransactionData

                emitState()

            } catch (e: CancellationException) {
                // Do nothing
            } catch (t: Throwable) {
//                Log.e("AAA", "fetchFinalQuote error", t)
            }
        }
    }

    fun setSwapSettings(settings: Map<String, Any?>) {
        this.swapSettings = settings
    }

    fun setSendTransactionSettings(sendTransactionSettings: SendTransactionSettings?) {
        this.sendTransactionSettings = sendTransactionSettings

        fetchFinalQuote()
    }

    fun setSwapProviderQuote(quote: SwapProviderQuote?) {
        this.quote = quote
    }

    fun start() {
        confirmInProgress = true
        emitState()

        fetchFinalQuote()
    }

    fun stop() {
        confirmInProgress = false
        emitState()
    }


    data class State(
        val amountOut: BigDecimal?,
        val amountOutMin: BigDecimal?,
        val cautions: List<HSCaution>,
        val quoteFields: List<DataField>,
        val priceImpact: BigDecimal?,
        val sendTransactionData: SendTransactionData?,
        val confirmInProgress: Boolean
    )
}
