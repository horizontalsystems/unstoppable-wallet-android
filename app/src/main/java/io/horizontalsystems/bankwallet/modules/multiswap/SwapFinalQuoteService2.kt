package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.ServiceState
import io.horizontalsystems.bankwallet.modules.multiswap.sendtransaction.SendTransactionSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class SwapFinalQuoteService2(
) : ServiceState<SwapFinalQuoteService2.State>() {
    private var swapSettings: Map<String, Any?> = mapOf()
    private var quote: SwapProviderQuote? = null

    private var sendTransactionSettings: SendTransactionSettings? = null
    private var finalQuote: SwapFinalQuote? = null
    private var finalQuoteCreatedAt: Long? = null
    private var confirmInProgress = false
    private var fetchFinalQuoteJob: Job? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun createState() = State(
        confirmInProgress = confirmInProgress,
        finalQuote = finalQuote,
        finalQuoteCreatedAt = finalQuoteCreatedAt
    )

    private fun fetchFinalQuote() {
        fetchFinalQuoteJob?.cancel()
        fetchFinalQuoteJob = coroutineScope.launch(Dispatchers.Default) {
            try {
                val quote = quote ?: return@launch

                finalQuote = quote.provider.fetchFinalQuote(quote.tokenIn, quote.tokenOut, quote.amountIn, swapSettings, sendTransactionSettings)
                finalQuoteCreatedAt = System.currentTimeMillis()

                ensureActive()
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
        finalQuote = null

        emitState()
    }

    fun refresh() {
        fetchFinalQuote()
    }


    data class State(
        val confirmInProgress: Boolean,
        val finalQuote: SwapFinalQuote?,
        val finalQuoteCreatedAt: Long?
    )
}
