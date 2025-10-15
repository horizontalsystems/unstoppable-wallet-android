package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellLeftImage
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.HSString
import io.horizontalsystems.bankwallet.uiv3.components.cell.ImageType
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketCoin(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    coinIconUrl: String,
    alternativeCoinIconUrl: String? = null,
    coinIconPlaceholder: Int,
    value: String? = null,
    marketDataValue: MarketDataValue? = null,
    label: String? = null,
    advice: Advice? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = onClick ?: { },
                onLongClick = onLongClick
            )
            .background(ComposeAppTheme.colors.lawrence)
    ) {
        CellPrimary(
            left = {
                CellLeftImage(
                    type = ImageType.Ellipse,
                    size = 32,
                    painter = rememberAsyncImagePainter(
                        model = coinIconUrl,
                        error = alternativeCoinIconUrl?.let { alternativeUrl ->
                            rememberAsyncImagePainter(
                                model = alternativeUrl,
                                error = painterResource(coinIconPlaceholder)
                            )
                        } ?: painterResource(coinIconPlaceholder)
                    ),
                )
            },
            middle = {
                CellMiddleInfo(
                    title = title.hs,
                    badge = advice?.name?.hs,
                    subtitle = subtitle.hs,
                    subtitleBadge = label?.hs,
                )
            },
            right = {
                CellRightInfo(
                    title = value?.hs ?: "n/a".hs,
                    subtitle = marketDataValueComponent(marketDataValue)
                )
            },
        )
    }
}

@Composable
fun SignalBadge(advice: Advice) {
    val textColor = when (advice) {
        Advice.Buy -> ComposeAppTheme.colors.remus
        Advice.Sell -> ComposeAppTheme.colors.lucian
        Advice.StrongBuy -> ComposeAppTheme.colors.tyler
        Advice.StrongSell -> ComposeAppTheme.colors.tyler
        Advice.Neutral -> ComposeAppTheme.colors.leah
        else -> ComposeAppTheme.colors.jacob
    }

    val backgroundColor = when (advice) {
        Advice.Buy -> ComposeAppTheme.colors.green20
        Advice.Sell -> ComposeAppTheme.colors.red20
        Advice.StrongBuy -> ComposeAppTheme.colors.remus
        Advice.StrongSell -> ComposeAppTheme.colors.lucian
        Advice.Neutral -> ComposeAppTheme.colors.blade
        else -> ComposeAppTheme.colors.yellow20
    }

    val text = when (advice) {
        Advice.Buy -> stringResource(R.string.Coin_Analytics_Indicators_Buy)
        Advice.Sell -> stringResource(R.string.Coin_Analytics_Indicators_Sell)
        Advice.StrongBuy -> stringResource(R.string.Coin_Analytics_Indicators_StrongBuy)
        Advice.StrongSell -> stringResource(R.string.Coin_Analytics_Indicators_StrongSell)
        Advice.Neutral -> stringResource(R.string.Coin_Analytics_Indicators_Neutral)
        else -> stringResource(R.string.Coin_Analytics_Indicators_Risky)
    }

    BadgeText(
        text = text,
        textColor = textColor,
        background = backgroundColor
    )
}

@Composable
fun marketDataValueComponent(marketDataValue: MarketDataValue?): HSString {
    return when (marketDataValue) {
        is MarketDataValue.MarketCap -> marketDataValue.value.hs

        is MarketDataValue.Volume -> marketDataValue.value.hs

        is MarketDataValue.Diff -> formatValueAsDiff(marketDataValue.value).hs(
            diffColor(marketDataValue.value.raw())
        )

        null -> "---".hs
    }
}

@Preview
@Composable
fun PreviewMarketCoin() {
    ComposeAppTheme {
        MarketCoin(
            title = "ETH",
            subtitle = "Ethereum With very long name for token",
            coinIconUrl = "eth.png",
            coinIconPlaceholder = R.drawable.logo_ethereum_24,
            value = "$2600",
        )
    }
}
