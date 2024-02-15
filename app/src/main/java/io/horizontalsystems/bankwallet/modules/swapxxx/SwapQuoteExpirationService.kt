package io.horizontalsystems.bankwallet.modules.swapxxx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SwapQuoteExpirationService {
    private var quote: SwapProviderQuote? = null

    private val _quoteExpiredFlow = MutableStateFlow(false)
    val quoteExpiredFlow = _quoteExpiredFlow.asStateFlow()

    private var coroutineScope = CoroutineScope(Dispatchers.Default)
    private var scheduleExpireQuoteJob: Job? = null

    fun setQuote(quote: SwapProviderQuote?) {
        this.quote = quote

        emitState()

        scheduleExpireQuote()
    }

    private fun emitState() {
        val expireAt = quote?.expireAt

        _quoteExpiredFlow.update {
            expireAt != null && expireAt <= System.currentTimeMillis()
        }
    }

    private fun scheduleExpireQuote() {
        scheduleExpireQuoteJob?.cancel()
        quote?.let {
            val remaining = it.expireAt - System.currentTimeMillis()
            scheduleExpireQuoteJob = coroutineScope.launch {
                delay(remaining)
                ensureActive()
                emitState()
            }
        }
    }
}
