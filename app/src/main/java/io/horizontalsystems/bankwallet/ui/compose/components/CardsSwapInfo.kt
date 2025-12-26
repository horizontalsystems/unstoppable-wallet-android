package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun CardsSwapInfo(content: @Composable() (ColumnScope.() -> Unit)) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(0.5.dp, ComposeAppTheme.colors.blade, RoundedCornerShape(16.dp))
            .padding(vertical = 2.dp),
        content = content
    )
}
