package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Single-flight launcher for button clicks that start a coroutine.
 *
 * [run] disables re-entry until the running block completes, which prevents a
 * same-frame double tap from launching the work twice (the button's `enabled`
 * gate only updates on the next recomposition, so an in-handler guard is still
 * required). Use [inProgress] to drive the button's `enabled`/title.
 */
@Stable
class AsyncAction(private val scope: CoroutineScope) {
    var inProgress by mutableStateOf(false)
        private set

    fun run(block: suspend () -> Unit) {
        if (inProgress) return
        inProgress = true
        scope.launch {
            try {
                block()
            } finally {
                inProgress = false
            }
        }
    }
}

@Composable
fun rememberAsyncAction(): AsyncAction {
    val scope = rememberCoroutineScope()
    return remember { AsyncAction(scope) }
}
