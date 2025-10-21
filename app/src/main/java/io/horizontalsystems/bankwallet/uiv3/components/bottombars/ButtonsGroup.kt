package io.horizontalsystems.bankwallet.uiv3.components.bottombars

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun ButtonsGroupVertical(content: @Composable (ColumnScope.() -> Unit)) {
    Column(
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 16.dp),
        content = content
    )
}

@Preview
@Composable
fun Preview_ButtonsGroup() {
    ComposeAppTheme {
//        ButtonsGroupVertical()
    }
}