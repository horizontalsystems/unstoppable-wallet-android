package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun HSRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    borderTop: Boolean = false,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        if (borderTop) {
            HsDivider(modifier = Modifier.align(Alignment.TopCenter))
        }

        if (borderBottom) {
            HsDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }

        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content,
        )
    }
}