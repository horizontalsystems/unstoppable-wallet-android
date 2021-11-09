package io.horizontalsystems.bankwallet.ui.compose

import androidx.compose.runtime.Composable
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
