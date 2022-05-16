package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
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
    MultilineClear(
        onClick = onClick,
        borderBottom = true
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            placeholder = coinIconPlaceholder,
            modifier = Modifier
                .padding(end = 16.dp)
                .size(24.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            MarketCoinFirstRow(coinName, coinRate)
            Spacer(modifier = Modifier.height(3.dp))
            MarketCoinSecondRow(coinCode, marketDataValue, label)
        }
    }
}

@Composable
fun MultilineClear(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderBottom: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clickable(
                enabled = onClick != null,
                onClick = { onClick?.invoke() }
            )
    ) {
        if (borderBottom) {
            Divider(
                thickness = 1.dp,
                color = ComposeAppTheme.colors.steel10,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
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
    Box(
        modifier = Modifier
            .height(60.dp)
            .background(ComposeAppTheme.colors.tyler)
            .clickable { onClick?.invoke() },
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = coinIconUrl,
                placeholder = coinIconPlaceholder,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp)
            )
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                MarketCoinFirstRow(coinName, coinRate)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(coinCode, marketDataValue, label)
            }
        }
    }
}

@Composable
fun MarketCoinFirstRow(coinName: String, rate: String?) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f).padding(end = 16.dp),
            text = coinName,
            color = ComposeAppTheme.colors.oz,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        rate?.let {
            Text(
                text = rate,
                color = ComposeAppTheme.colors.leah,
                style = ComposeAppTheme.typography.body,
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
        Text(
            text = subtitle,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
            maxLines = 1,
        )
        marketDataValue?.let {
            Spacer(modifier = Modifier.weight(1f))
            MarketDataValueComponent(marketDataValue)
        }
    }
}

@Composable
fun MarketDataValueComponent(marketDataValue: MarketDataValue) {
    when (marketDataValue) {
        is MarketDataValue.MarketCap -> {
            Row {
                Text(
                    text = "MCap",
                    color = ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = marketDataValue.value,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
            }
        }
        is MarketDataValue.Volume -> {
            Row {
                Text(
                    text = "Vol",
                    color = ComposeAppTheme.colors.jacob,
                    style = ComposeAppTheme.typography.subhead2,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = marketDataValue.value,
                    color = ComposeAppTheme.colors.grey,
                    style = ComposeAppTheme.typography.subhead2,
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
            "Ethereum",
            "ETH",
            "eth.png",
            R.drawable.logo_ethereum_24,
            "$2600",
        )
    }
}
