package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineClear
import io.horizontalsystems.bankwallet.ui.compose.components.Description
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah

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