package io.horizontalsystems.bankwallet.modules.settings.premiumbanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.subhead1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus

@Composable
fun PremiumBanner(onClick: () -> Unit) {
    val viewModel = viewModel<PremiumBannerViewModel>()
    PremiumBannerView(
        onClick = onClick,
        hasFreeTrial = viewModel.uiState.hasFreeTrial
    )
}


@Composable
fun PremiumBannerView(
    onClick: () -> Unit,
    hasFreeTrial: Boolean
) {
    val darkTheme = isSystemInDarkTheme()

    val radialGradient = Brush.linearGradient(
        colors = if (darkTheme) {
            listOf(
                Color(0xCCEDD716),
                Color(0x4DFF9B26),
                Color(0x000F1014),
                Color(0x000F1014),
            )
        } else {
            listOf(
                Color(0xCCF5DE15),
                Color(0x4DFFA726),
                Color(0x00E1E1E5),
                Color(0x00E1E1E5),
            )
        },
        start = Offset(0f, Float.POSITIVE_INFINITY),
        end = Offset(Float.POSITIVE_INFINITY, 0f)
    )

    val topRightRadialGradient = Brush.linearGradient(
        colors = if (darkTheme) {
            listOf(
                Color(0x66003C74),
                Color(0x000F1014),
            )
        } else {
            listOf(
                Color(0x66AFC7E8),
                Color(0x00E1E1E5),
            )
        },
        start = Offset(Float.POSITIVE_INFINITY, 0f),
        end = Offset(0f, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.jeremy)
            .height(IntrinsicSize.Max)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
            )
            Box(
                modifier = Modifier
                    .weight(5f)
                    .fillMaxHeight()
                    .background(topRightRadialGradient)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .background(radialGradient)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .padding(vertical = 16.dp),
            ) {
                headline1_jacob(stringResource(R.string.SettingsBanner_Premium))
                VSpacer(20.dp)
                Column(
                    modifier = Modifier.defaultMinSize(minHeight = 60.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    subhead1_leah(stringResource(R.string.SettingsBanner_PremiumBannerDescription))
                    VSpacer(4.dp)
                    if (hasFreeTrial) {
                        subhead2_remus(stringResource(R.string.SettingsBanner_PremiumTryFee))
                    }
                }
            }

            HSpacer(10.dp)

            Image(
                painter = painterResource(R.drawable.premium_banner_star),
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(118.dp),
                contentDescription = null
            )
        }
    }
}

@Preview
@Composable
fun PremiumBannerPreview() {
    ComposeAppTheme {
        PremiumBannerView(
            onClick = {},
            hasFreeTrial = true
        )
    }
}