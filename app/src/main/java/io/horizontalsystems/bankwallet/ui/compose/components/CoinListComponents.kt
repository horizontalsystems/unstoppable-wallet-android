package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.modules.market.category.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Composable
fun MarketListCoin(
    coinName: String,
    coinCode: String,
    coinRate: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    marketDataValue: MarketDataValue,
    rank: String?,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .height(61.dp)
            .clickable { onClick?.invoke() }
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoinImage(
                iconUrl = coinIconUrl,
                placeholder = coinIconPlaceholder,
                modifier = Modifier.padding(horizontal = 16.dp).size(24.dp)
            )
            Column(
                modifier = Modifier.padding(end = 16.dp)
            ) {
                MarketCoinFirstRow(coinName, coinRate)
                Spacer(modifier = Modifier.height(3.dp))
                MarketCoinSecondRow(coinCode, marketDataValue, rank)
            }
        }
        Divider(
            thickness = 1.dp,
            color = ComposeAppTheme.colors.steel10,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun MarketCoinFirstRow(coinName: String, rate: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = coinName,
            color = ComposeAppTheme.colors.oz,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = rate,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
        )
    }
}

@Composable
fun MarketCoinSecondRow(coinCode: String, marketDataValue: MarketDataValue, rank: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        rank?.let { rank ->
            Box(
                modifier = Modifier.padding(end = 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ComposeAppTheme.colors.jeremy)
            ) {
                Text(
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 1.dp),
                    text = rank,
                    color = ComposeAppTheme.colors.bran,
                    style = ComposeAppTheme.typography.microSB,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = coinCode,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1f))
        MarketDataValueComponent(marketDataValue)
    }
}

@Composable
fun MarketDataValueComponent(marketDataValue: MarketDataValue){
    when(marketDataValue){
        is MarketDataValue.MarketCap -> {
            Row{
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
            Row{
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
    }
}

@Composable
fun ListLoadingView() {
    Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center).size(24.dp),
            color = ComposeAppTheme.colors.grey,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
fun ListErrorView(
    errorText: String,
    onClick: (() -> Unit)? = null
) {
    Box(modifier = Modifier.height(240.dp).fillMaxWidth()) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 16.dp)
                .clickable {
                    onClick?.invoke()
                },
            text = errorText,
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.subhead2,
        )
    }
}
