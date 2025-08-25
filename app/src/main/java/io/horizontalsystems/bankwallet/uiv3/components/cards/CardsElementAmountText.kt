package io.horizontalsystems.bankwallet.uiv3.components.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.bankwallet.ui.compose.components.DoubleText
import io.horizontalsystems.bankwallet.uiv3.components.HSPreview

@Composable
fun CardsElementAmountText(
    title: String,
    body: String,
    dimmed: Boolean,
    onClickTitle: () -> Unit,
    onClickSubtitle: () -> Unit,
) {
    DoubleText(
        title,
        body,
        dimmed,
        onClickTitle,
        onClickSubtitle,
    )
}

@Preview
@Composable
fun Preview_CardsElementAmountText() {
    HSPreview {
        CardsElementAmountText(
            title = "\$1,289,231.60",
            body = "≈22.6057 BTC",
            dimmed = false,
            onClickTitle = {},
            onClickSubtitle = {}
        )
    }
}

