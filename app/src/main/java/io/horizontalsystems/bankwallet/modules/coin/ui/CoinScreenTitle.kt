package io.horizontalsystems.bankwallet.modules.coin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.CoinImage

@Composable
fun CoinScreenTitle(coinName: String, marketCapRank: Int?, coinIconUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinImage(
            iconUrl = coinIconUrl,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = coinName,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        marketCapRank?.let { marketCapRank ->
            Text(
                text = "#$marketCapRank",
                modifier = Modifier.padding(start = 16.dp),
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.subhead1
            )
        }
    }
}

@Preview
@Composable
fun CoinScreenTitlePreviewNoRank() {
    ComposeAppTheme {
        CoinScreenTitle(
            coinName = "Synthetix Network TokenSynthetix Network Token",
            marketCapRank = null,
            coinIconUrl = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/bitcoin@3x.png"
        )
    }
}

@Preview
@Composable
fun CoinScreenTitlePreviewLongTitle() {
    ComposeAppTheme {
        CoinScreenTitle(
            coinName = "Synthetix Network Token Synthetix Network Token Synthetix Network Token Synthetix Network Token",
            marketCapRank = 123,
            coinIconUrl = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/bitcoin@3x.png"
        )
    }
}

@Preview
@Composable
fun CoinScreenTitlePreviewShortTitle() {
    ComposeAppTheme {
        CoinScreenTitle(
            coinName = "Bitcoin",
            marketCapRank = 1,
            coinIconUrl = "https://markets.nyc3.digitaloceanspaces.com/coin-icons/ios/bitcoin@3x.png"
        )
    }
}
