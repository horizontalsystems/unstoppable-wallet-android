package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellData2
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.DescriptionMarkdown

@Preview
@Composable
fun AboutPreview() {
    ComposeAppTheme {
        About(text = "Bitcoin is a cryptocurrency and worldwide payment system. It is the first decentralized digital currency, as the system works without a central bank or single administrator. Bitcoin is a cryptocurrency and worldwide payment system. It is the first decentralized digital currency, as the system works without a central bank or single administrator. Bitcoin is a cryptocurrency and worldwide payment system. It is the first decentralized digital currency, as the system works without a central bank or single administrator. Bitcoin is a cryptocurrency and worldwide payment system. It is the first decentralized digital currency, as the system works without a central bank or single administrator. Bitcoin is a cryptocurrency")
    }
}

@Composable
fun About(text: String) {
    Column {
        CellSingleLineClear(borderTop = true) {
            Text(
                text = stringResource(id = R.string.CoinPage_About),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        var showReadMoreToggle by remember { mutableStateOf(false)}
        var expanded by remember { mutableStateOf(false)}

        DescriptionMarkdown(
            textMaxLines = 8,
            toggleLines = 2,
            text = text,
            expanded = expanded
        ) { overflow ->
            showReadMoreToggle = overflow
        }

        if (showReadMoreToggle) {
            CellData2 {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val stringId = if (expanded) R.string.CoinPage_ReadLess else R.string.CoinPage_ReadMore
                    Text(
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 16.dp),
                        text = stringResource(id = stringId),
                        style = ComposeAppTheme.typography.subhead2,
                        color = ComposeAppTheme.colors.jacob,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}
