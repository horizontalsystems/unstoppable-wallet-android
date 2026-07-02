package io.horizontalsystems.core.uiv3.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.horizontalsystems.core.ui.compose.components.HsDivider

@Composable
fun BoxBordered(
    top: Boolean = false,
    bottom: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box {
        content()
        if (top) {
            HsDivider()
        }
        if (bottom) {
            HsDivider(Modifier.align(Alignment.BottomCenter))
        }
    }
}