package cash.p.terminal.modules.coin.overview.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cash.p.terminal.modules.coin.CoinDataItem
import cash.p.terminal.ui.compose.components.Badge
import io.horizontalsystems.core.CellSingleLineLawrenceSection
import cash.p.terminal.ui_compose.components.subhead1_leah
import cash.p.terminal.ui_compose.components.subhead2_grey

@Preview
@Composable
fun MarketDataPreview() {
    val marketData = listOf(
        CoinDataItem(title = "Market Cap", value = "$123.34 B", rankLabel = "#555"),
        CoinDataItem(title = "Trading Volume", value = "112,112,112,112,112,112,112,112,112,112,112,112,112,112,112,112 ETH"),
        CoinDataItem(title = "Inception Date", value = "Jul 23, 2012"),
    )

    cash.p.terminal.ui_compose.theme.ComposeAppTheme(darkTheme = false) {
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
            subhead2_grey(text = marketDataLine.title)

            marketDataLine.rankLabel?.let {
                Badge(modifier = Modifier.padding(start = 8.dp), it)
            }

            marketDataLine.value?.let { value ->
                subhead1_leah(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    text = value,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}