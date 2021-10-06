package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import java.math.BigDecimal

@Composable
fun MarketListCoin(
    coinName: String,
    coinCode: String,
    coinRate: String,
    coinIconUrl: String,
    coinIconPlaceholder: Int,
    rateDiff: BigDecimal?,
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
                MarketCoinSecondRow(coinCode, rateDiff, rank)
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
fun MarketCoinSecondRow(coinCode: String, rateDiff: BigDecimal?, rank: String?) {
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
        Text(
            text = RateText(rateDiff),
            color = RateColor(rateDiff),
            style = ComposeAppTheme.typography.subhead2,
            maxLines = 1,
        )
    }
}
