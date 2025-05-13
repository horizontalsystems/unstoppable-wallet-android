package io.horizontalsystems.bankwallet.modules.settings.banners

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.Bright
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.YellowD

@Composable
fun DonateBanner(onClick: () -> Unit) {

    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .paint(
                painter = painterResource(R.drawable.donate_banner_bg),
                contentScale = ContentScale.FillBounds
            )
            .clickable(onClick = onClick)
    ) {

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.6f)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.SettingsBanner_BePartOfTheFuture),
                    style = ComposeAppTheme.typography.headline1.copy(lineHeight = 22.sp),
                    color = Bright,
                )
                Text(
                    text = stringResource(R.string.SettingsBanner_SupportTheProject),
                    style = ComposeAppTheme.typography.subhead,
                    color = YellowD,
                )
            }

            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.4f)
            )
        }
    }
}

@Preview
@Composable
fun Preview_DonateBanner() {
    ComposeAppTheme {
        DonateBanner(
            onClick = {},
        )
    }
}