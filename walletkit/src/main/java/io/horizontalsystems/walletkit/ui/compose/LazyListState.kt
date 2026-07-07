package io.horizontalsystems.walletkit.ui.compose

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun hsRememberLazyListState(i: Int, vararg keys: Any?): LazyListState {
    val listState = rememberLazyListState()
    LaunchedEffect(keys = keys) {
        if (listState.firstVisibleItemIndex >= i) {
            listState.scrollToItem(i)
        }
    }

    return listState
}
