package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
            Text(
                text = stringResource(id = R.string.CoinPage_Categories),
                style = ComposeAppTheme.typography.body,
                color = ComposeAppTheme.colors.oz,
            )
        }

        Description(categories.joinToString())
    }
}