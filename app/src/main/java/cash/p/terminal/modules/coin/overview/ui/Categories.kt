package cash.p.terminal.modules.coin.overview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import cash.p.terminal.R
import cash.p.terminal.ui.compose.ComposeAppTheme
import cash.p.terminal.ui.compose.components.CellSingleLineClear
import cash.p.terminal.ui.compose.components.Description
import cash.p.terminal.ui.compose.components.body_leah

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
            body_leah(text = stringResource(id = R.string.CoinPage_Categories))
        }

        Description(categories.joinToString())
    }
}