package io.horizontalsystems.bankwallet.modules.coin.analytics.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.analytics.title
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.cell.CellUniversal
import io.horizontalsystems.marketkit.models.Analytics.TechnicalAdvice.Advice

@Composable
fun TechnicalAdviceBlock(
    detailText: String,
    advice: Advice?
) {
    var showDetails by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(height = 24.dp)
        AdviceMeter(
            advice = advice,
            title = advice?.title,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        VSpacer(height = 24.dp)
        CellUniversal(
            onClick = {
                showDetails = !showDetails
            }
        ) {
            body_leah(
                text = stringResource(R.string.TechnicalAdvice_Details),
                modifier = Modifier.weight(1f)
            )
            Icon(
                modifier = Modifier.padding(start = 8.dp),
                painter = painterResource(if (showDetails) R.drawable.ic_arrow_big_up_20 else R.drawable.ic_arrow_big_down_20),
                contentDescription = null,
                tint = ComposeAppTheme.colors.grey
            )
        }

        if (showDetails) {
            body_leah(
                text = detailText,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            VSpacer(height = 12.dp)
        }
        caption_grey(
            text = stringResource(R.string.TechnicalAdvice_IndicatorsDisclaimer),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        VSpacer(height = 16.dp)
    }
}

@Composable
private fun AdviceMeter(
    advice: Advice?,
    title: String?,
    modifier: Modifier = Modifier
) {
    val needleImage: Int? = when (advice) {
        Advice.Buy -> R.drawable.indicator_needle_buy
        Advice.Sell -> R.drawable.indicator_needle_sell
        Advice.Neutral -> R.drawable.indicator_needle_neutral
        Advice.StrongBuy -> R.drawable.indicator_needle_strongbuy
        Advice.StrongSell -> R.drawable.indicator_needle_strongsell
        else -> null
    }
    val baseImage: Int = when (advice) {
        Advice.Oversold,
        Advice.Overbought -> R.drawable.indicator_base_risky

        Advice.Sell,
        Advice.Buy,
        Advice.StrongBuy,
        Advice.StrongSell,
        Advice.Neutral -> R.drawable.indicator_base_normal

        else -> R.drawable.indicator_base_locked
    }
    val textBackground = when (advice) {
        Advice.StrongSell -> Color(0xFFF43A4F)
        Advice.Sell -> Color(0x80F43A4F)
        Advice.Neutral -> ComposeAppTheme.colors.steel20
        Advice.Buy -> Color(0x8013D670)
        Advice.StrongBuy -> Color(0xFF13D670)
        else -> ComposeAppTheme.colors.yellow20
    }
    val textColor = when (advice) {
        Advice.StrongBuy,
        Advice.Buy,
        Advice.Neutral,
        Advice.Sell -> ComposeAppTheme.colors.leah

        Advice.StrongSell -> ComposeAppTheme.colors.tyler
        else -> ComposeAppTheme.colors.jacob
    }
    Box(
        modifier = modifier.size(width = 240.dp, height = 126.dp)
    ) {
        Image(
            painter = painterResource(id = baseImage),
            contentDescription = null,
            modifier = Modifier.size(240.dp, 126.dp)
        )
        needleImage?.let {
            Icon(
                painter = painterResource(id = it),
                contentDescription = null,
                modifier = Modifier.size(240.dp, 126.dp),
                tint = ComposeAppTheme.colors.leah
            )
        }
        title?.let { adviceTitle ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(textBackground)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
            ) {
                Text(
                    text = adviceTitle,
                    color = textColor,
                    style = ComposeAppTheme.typography.captionSB
                )
            }
        }
    }
}

@Preview
@Composable
private fun TechnicalAdviceMeterPreview() {
    ComposeAppTheme {
        AdviceMeter(advice = Advice.Sell, title = "Sell")
    }
}