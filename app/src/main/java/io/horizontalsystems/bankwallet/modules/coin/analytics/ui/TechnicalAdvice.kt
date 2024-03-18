package io.horizontalsystems.bankwallet.modules.coin.analytics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.caption_grey
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_bran
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah

@Composable
fun TechnicalAdviceBlock(
    adviceTitle: String,
    detailText: String,
    sliderPosition: Int
) {
    var showDetails by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        headline1_bran(text = adviceTitle)
        VSpacer(height = 12.dp)
        AdviceSlider(sliderPosition)
        VSpacer(height = 12.dp)
        if (showDetails) {
            body_leah(text = detailText)
            VSpacer(height = 12.dp)
        }
        caption_grey(text = stringResource(R.string.TechnicalAdvice_IndicatorsDisclaimer))
        VSpacer(height = 16.dp)
        Box(
            modifier = Modifier
                .height(48.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            headline2_leah(
                text = stringResource(if (showDetails) R.string.TechnicalAdvice_ShowDetails else R.string.TechnicalAdvice_HideDetails),
                modifier = Modifier
                    .clickable {
                        showDetails = !showDetails
                    },
            )
        }

    }
}

@Composable
private fun AdviceSlider(sliderPosition: Int) {
    val thumbSize = 32.dp
    val color = when (sliderPosition) {
        0 -> Color(0xFFF43A4F)
        1 -> Color(0xFFF5A840)
        2 -> Color(0xFFA8DD26)
        3 -> Color(0xFF05C46B)
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x33F43A4F),
                        Color(0x33FFD600),
                        Color(0x3305C46B),
                    )
                )
            )
            .padding(2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            for (i in 0..3) {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            color = if (i == sliderPosition) color else Color.Transparent
                        )
                )
            }
        }
    }
}
