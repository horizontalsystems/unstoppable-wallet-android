package io.horizontalsystems.bankwallet.uiv3.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs

@Composable
fun CardsElementAmountText(
    title: HSString,
    body: HSString,
    onClickTitle: () -> Unit,
    onClickSubtitle: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComposeAppTheme.colors.tyler)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickTitle
                ),
            text = title.text,
            style = ComposeAppTheme.typography.title2М,
            color = title.color ?: ComposeAppTheme.colors.leah,
            maxLines = 1
        )
        Text(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClickSubtitle
                ),
            text = body.text,
            style = ComposeAppTheme.typography.body,
            color = body.color ?: ComposeAppTheme.colors.grey,
            maxLines = 1
        )
    }
}

@Preview
@Composable
fun Preview_CardsElementAmountText() {
    ComposeAppTheme {
        CardsElementAmountText(
            title = "\$1,289,231.60".hs,
            body = "≈22.6057 BTC".hs,
            onClickTitle = {},
            onClickSubtitle = {}
        )
    }
}

