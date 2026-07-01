package io.horizontalsystems.bankwallet.modules.coin.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HsImage
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.body_grey
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_grey

@Composable
fun CoinScreenTitle(
    coinName: String,
    marketCapRank: Int?,
    coinIconUrl: String,
    alternativeCoinIconUrl: String?,
    iconPlaceholder: Int?
) {
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        HsImage(
            url = coinIconUrl,
            alternativeUrl = alternativeCoinIconUrl,
            placeholder = iconPlaceholder,
            modifier = Modifier.size(32.dp).clip(CircleShape)
        )

        body_grey(
            text = coinName,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        marketCapRank?.let { marketCapRank ->
            subhead1_grey(
                text = "#$marketCapRank",
                modifier = Modifier.padding(start = 16.dp),
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
            coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/bitcoin@3x.png",
            alternativeCoinIconUrl = null,
            iconPlaceholder = null
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
            coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/bitcoin@3x.png",
            alternativeCoinIconUrl = null,
            iconPlaceholder = null
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
            coinIconUrl = "https://cdn.blocksdecoded.com/coin-icons/32px/bitcoin@3x.png",
            alternativeCoinIconUrl = null,
            iconPlaceholder = null
        )
    }
}
