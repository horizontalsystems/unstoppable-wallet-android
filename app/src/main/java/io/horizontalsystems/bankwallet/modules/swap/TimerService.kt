package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.concurrent.Executors
import kotlin.concurrent.schedule

class TimerService(private val evmKit: EthereumKit) {
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val coroutineScope = CoroutineScope(singleDispatcher)

    private var lastBlockDisposable: Disposable? = null
    private var timer: Timer? = null
    private val timeoutPeriodSeconds = evmKit.chain.syncInterval
    private val timeoutProgressStep = 1f / (timeoutPeriodSeconds * 2)

    val reSyncFlow = MutableSharedFlow<Unit>()

    private val _timeoutProgressFlow = MutableStateFlow(0f)
    val timeoutProgressFlow: StateFlow<Float>
        get() = _timeoutProgressFlow

    fun start() {
        sync()
        lastBlockDisposable = evmKit.lastBlockHeightFlowable
            .subscribeOn(Schedulers.io())
            .subscribe {
                sync()
            }
    }

    fun stop() {
        lastBlockDisposable?.dispose()
        lastBlockDisposable = null
        stopTimer()
    }

    private fun sync() {
        coroutineScope.launch {
            reSyncFlow.emit(Unit)
            resetTimer()
        }
    }

    private fun startTimer() {
        _timeoutProgressFlow.update { 1f }

        timer = Timer().apply {
            schedule(0, 500) {
                onFireTimer()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
    }

    private fun resetTimer() {
        stopTimer()
        startTimer()
    }

    private fun onFireTimer() {
        val currentTimeoutProgress = _timeoutProgressFlow.value
        val newTimeoutProgress = currentTimeoutProgress - timeoutProgressStep

        _timeoutProgressFlow.update { newTimeoutProgress.coerceAtLeast(0f) }
    }
}
