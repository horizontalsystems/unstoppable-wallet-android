package io.horizontalsystems.bankwallet.uiv3.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun HSPreview(content: @Composable() (ColumnScope.() -> Unit)) {
    ComposeAppTheme(darkTheme = false) {
        Column(
            modifier = Modifier
                .background(ComposeAppTheme.colors.tyler)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
