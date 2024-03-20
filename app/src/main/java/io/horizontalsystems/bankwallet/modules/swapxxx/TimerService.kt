package io.horizontalsystems.bankwallet.modules.swapxxx

import android.os.CountDownTimer
import io.horizontalsystems.bankwallet.core.ServiceState
import kotlin.math.ceil

class TimerService : ServiceState<TimerService.State>() {
    private var expirationTimer: CountDownTimer? = null

    private var secondsRemaining: Long? = null
    private var timeout = false

    override fun createState() = State(
        remaining = secondsRemaining,
        timeout = timeout
    )

    fun start(seconds: Long) {
        secondsRemaining = seconds
        timeout = false

        emitState()

        runExpirationTimer(
            millisInFuture = seconds * 1000,
            onTick = { millisUntilFinished ->
                secondsRemaining = ceil(millisUntilFinished / 1000.0).toLong()
                emitState()
            },
            onFinish = {
                timeout = true
                emitState()
            }
        )
    }

    fun stop() {
        expirationTimer?.cancel()
    }

    private fun runExpirationTimer(millisInFuture: Long, onTick: (Long) -> Unit, onFinish: () -> Unit) {
        expirationTimer?.cancel()
        expirationTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) = onTick.invoke(millisUntilFinished)
            override fun onFinish() = onFinish.invoke()
        }
        expirationTimer?.start()
    }

    fun reset() {
        expirationTimer?.cancel()
        secondsRemaining = null
        timeout = false

        emitState()
    }

    data class State(val remaining: Long?, val timeout: Boolean)
}
