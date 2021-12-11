package io.horizontalsystems.bankwallet.modules.coin.overview.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.Badge
import io.horizontalsystems.bankwallet.ui.compose.components.CellSingleLineLawrenceSection

@Preview
@Composable
fun MarketDataPreview() {
    val marketData = listOf(
        CoinDataItem(title = "Market Cap", value = "$123.34 B", rankLabel = "#555"),
        CoinDataItem(title = "Trading Volume", value = "112,112,112,112,112,112,112,112,112,112,112,112,112,112,112,112 ETH"),
        CoinDataItem(title = "Inception Date", value = "Jul 23, 2012"),
    )

    ComposeAppTheme(darkTheme = false) {
        MarketData(marketData)
    }
}

@Composable
fun MarketData(marketData: List<CoinDataItem>) {
    CellSingleLineLawrenceSection(marketData) { marketDataLine ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = marketDataLine.title,
                style = ComposeAppTheme.typography.subhead2,
                color = ComposeAppTheme.colors.grey
            )

            marketDataLine.rankLabel?.let {
                Badge(modifier = Modifier.padding(start = 8.dp), it)
            }

            marketDataLine.value?.let { value ->
                Text(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    text = value,
                    style = ComposeAppTheme.typography.subhead1,
                    color = ComposeAppTheme.colors.oz,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}