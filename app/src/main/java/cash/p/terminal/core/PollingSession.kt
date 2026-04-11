package cash.p.terminal.core

import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import java.util.concurrent.atomic.AtomicInteger

internal inline fun AtomicInteger.onPollingStarted(action: () -> Unit) {
    incrementAndGet()
    try {
        action()
    } catch (throwable: Throwable) {
        decrementAndGet()
        throw throwable
    }
}

internal suspend inline fun AtomicInteger.onPollingStartedSuspend(
    crossinline action: suspend () -> Unit,
) {
    incrementAndGet()
    try {
        action()
    } catch (throwable: Throwable) {
        decrementAndGet()
        throw throwable
    }
}

internal inline fun AtomicInteger.onPollingStopped(
    backgroundManager: BackgroundManager,
    action: () -> Unit,
) {
    try {
        if (backgroundManager.stateFlow.value != BackgroundManagerState.EnterForeground) {
            action()
        }
    } finally {
        decrementAndGet()
    }
}

internal suspend inline fun AtomicInteger.onPollingStoppedSuspend(
    backgroundManager: BackgroundManager,
    crossinline action: suspend () -> Unit,
) {
    try {
        if (backgroundManager.stateFlow.value != BackgroundManagerState.EnterForeground) {
            action()
        }
    } finally {
        decrementAndGet()
    }
}
