package io.horizontalsystems.walletkit.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Collects the flow in [scope]. Kit-free replacement for the Zcash SDK's collectWith. */
fun <T> Flow<T>.collectWith(scope: CoroutineScope, action: suspend (T) -> Unit) {
    scope.launch {
        collect { action(it) }
    }
}

/** Runs [action] on the first emission only. Kit-free replacement for the SDK's onFirstWith. */
fun <T> Flow<T>.onFirstWith(scope: CoroutineScope, action: suspend (T) -> Unit) {
    scope.launch {
        onEach { action(it) }.first()
    }
}
