package io.horizontalsystems.bankwallet.modules.settings.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight.Companion.W500
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.highlightText
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_remus


@Composable
fun PremiumBanner(onClick: () -> Unit) {
    val bodyText = highlightText(
        text = stringResource(R.string.SettingsBanner_PremiumBannerDescription),
        textColor = ComposeAppTheme.colors.leah,
        highlightPart = stringResource(R.string.SettingsBanner_PremiumBannerHighlightedWord),
        highlightColor = ComposeAppTheme.colors.jacob
    )

    val darkTheme: Boolean = isSystemInDarkTheme()

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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp)
                    .padding(start = 16.dp)
                    .padding(end = 8.dp)
            ) {
                headline1_leah(
                    stringResource(R.string.SettingsBanner_GetPremium)
                )

                VSpacer(12.dp)

                AutoSizedText(bodyText)

                VSpacer(12.dp)

                subhead2_remus(stringResource(R.string.SettingsBanner_FreeFor7Days))
            }

            Image(
                painter = painterResource(R.drawable.premium_banner_star),
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(118.dp),
                contentDescription = null
            )

        }
    }
}

@Composable
private fun AutoSizedText(
    bodyText: AnnotatedString,
    maxLines: Int = 2,
    initialFontSize: Float = 16f,
    minFontSize: Float = 8f,
    modifier: Modifier = Modifier
) {
    var fontSize by remember { mutableFloatStateOf(initialFontSize) }
    var isTextReady by remember { mutableStateOf(false) }

    Box(modifier = modifier) {

        Text(
            text = bodyText,
            modifier = Modifier
                .alpha(0f)
                .fillMaxWidth(),
            fontSize = fontSize.sp,
            fontWeight = W500,
            maxLines = maxLines,
            onTextLayout = { textLayoutResult ->
                if (textLayoutResult.didOverflowHeight || textLayoutResult.didOverflowWidth) {
                    if (fontSize > minFontSize) {
                        fontSize -= 1f
                    } else {
                        isTextReady = true
                    }
                } else {
                    isTextReady = true
                }
            }
        )

        // Visible TextField only when size is ready
        if (isTextReady) {
            Text(
                text = bodyText,
                modifier = Modifier.fillMaxWidth(),
                fontSize = fontSize.sp,
                maxLines = maxLines,
                color = Color.White,
            )
        }
    }
}

@Preview
@Composable
fun PremiumBannerPreview() {
    ComposeAppTheme {
        PremiumBanner {}
    }
}