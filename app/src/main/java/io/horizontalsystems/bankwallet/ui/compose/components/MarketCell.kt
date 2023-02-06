package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun MarketCoinClear(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    SectionItemBorderedRowUniversalClear(
        onClick = onClick,
        borderBottom = true
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinCode, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinName, marketDataValue, label)
        }
    }
}

@Composable
fun MarketCoin(
    coinName: String,
    coinCode: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    coinRate: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    onClick: (() -> Unit)? = null
) {
    RowUniversal(
        modifier = Modifier
            .background(ComposeAppTheme.colors.tyler)
            .padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(32.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinCode, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinName, marketDataValue, label)
        }
    }
}

@Composable
fun MarketCoinFirstRow(title: String, rate: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        body_leah(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        rate?.let {
            body_leah(
                text = rate,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MarketCoinSecondRow(
    subtitle: String,
    marketDataValue: MarketDataValue?,
    label: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        label?.let {
            Badge(
                modifier = Modifier.padding(end = 8.dp),
                text = it
            )
        }
        subhead2_grey(
            text = subtitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        marketDataValue?.let {
            Spacer(Modifier.width(8.dp))
            MarketDataValueComponent(marketDataValue)
        }
    }
}

@Composable
fun MarketDataValueComponent(marketDataValue: MarketDataValue) {
    when (marketDataValue) {
        is MarketDataValue.MarketCap -> {
            Row {
                subhead2_grey(
                    text = marketDataValue.value,
                    maxLines = 1,
                )
            }
        }
        is MarketDataValue.Volume -> {
            Row {
                subhead2_grey(
                    text = marketDataValue.value,
                    maxLines = 1,
                )
            }
        }
        is MarketDataValue.Diff -> {
            Text(
                text = RateText(marketDataValue.value),
                color = RateColor(marketDataValue.value),
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
        is MarketDataValue.DiffNew -> {
            Text(
                text = formatValueAsDiff(marketDataValue.value),
                color = diffColor(marketDataValue.value.raw()),
                style = ComposeAppTheme.typography.subhead2,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
fun PreviewMarketCoin(){
    ComposeAppTheme {
        MarketCoin(
            coinName = "Ethereum With very long name for token",
            coinCode = "ETH",
            coinIconUrl = "eth.png",
            coinIconPlaceholder = R.drawable.logo_ethereum_24,
            coinRate = "$2600",
        )
    }
}
