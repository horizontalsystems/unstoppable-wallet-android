package io.horizontalsystems.bankwallet.modules.swapxxx

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class SwapQuoteExpirationService {
    private var quote: SwapProviderQuote? = null

    private val _quoteExpiredFlow = MutableSharedFlow<Boolean>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val quoteExpiredFlow = _quoteExpiredFlow.asSharedFlow()

    private var coroutineScope = CoroutineScope(Dispatchers.Default)
    private var scheduleExpireQuoteJob: Job? = null

    fun setQuote(quote: SwapProviderQuote?) {
        this.quote = quote

        emitState()

        scheduleExpireQuote()
    }

    private fun emitState() {
        val expireAt = quote?.expireAt
        _quoteExpiredFlow.tryEmit(expireAt != null && expireAt <= System.currentTimeMillis())
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
