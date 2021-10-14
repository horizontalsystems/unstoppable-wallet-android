package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.Description

@Preview
@Composable
fun CategoriesPreview() {
    ComposeAppTheme {
        val categories = listOf("DEXes", "Synthetics", "Algo Stablecoins")
        Categories(categories = categories)
    }
}

@Composable
fun Categories(categories: List<String>) {
    Column {
        CellSingleLineClear(borderTop = true) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.CoinPage_Categories),
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.oz,
                )
            }
        }

        Description(categories.joinToString())
    }
}