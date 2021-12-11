package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.SwipeRefreshState

@Composable
fun HSSwipeRefresh(
    state: SwipeRefreshState,
    onRefresh: () -> Unit,
    swipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SwipeRefresh(
        modifier = Modifier.fillMaxSize(),
        state = state,
        onRefresh = onRefresh,
        swipeEnabled = swipeEnabled,
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                scale = true,
                backgroundColor = ComposeAppTheme.colors.claude,
                contentColor = ComposeAppTheme.colors.oz,
            )
        },
        content = content
    )
}
