package io.horizontalsystems.bankwallet.modules.settings.banners

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.highlightText
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun BlackFridayBanner(onClick: () -> Unit) {
    BlackFridayBannerView(
        onClick = onClick,
    )
}

@Composable
fun BlackFridayBannerView(
    onClick: () -> Unit,
) {

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .paint(
                painter = painterResource(
                    if (isSystemInDarkTheme()) {
                        R.drawable.bf_banner_backround_dark
                    } else {
                        R.drawable.bf_banner_backround_light
                    }
                ),
                contentScale = ContentScale.FillBounds
            )
            .clickable(onClick = onClick)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VSpacer(16.dp)
            Image(
                modifier = Modifier.padding(horizontal = 16.dp),
                contentScale = ContentScale.FillBounds,
                painter = painterResource(R.drawable.blackfriday_banner),
                contentDescription = null
            )
            VSpacer(6.dp)
            val text = highlightText(
                text = stringResource(R.string.Premium_Banner_BlackFridayText),
                textColor = ComposeAppTheme.colors.leah,
                highlightPart = stringResource(R.string.Premium_Title),
                highlightColor = ComposeAppTheme.colors.jacob
            )
            Text(
                text = text,
                style = ComposeAppTheme.typography.subhead,
                color = ComposeAppTheme.colors.leah,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Preview
@Composable
fun Preview_BlackFridayBannerView() {
    ComposeAppTheme {
        BlackFridayBannerView(
            onClick = {},
        )
    }
}
