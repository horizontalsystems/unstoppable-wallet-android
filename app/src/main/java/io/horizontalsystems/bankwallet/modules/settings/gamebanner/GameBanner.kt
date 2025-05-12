package io.horizontalsystems.bankwallet.modules.settings.gamebanner

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.Bright
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_jacob

@Composable
fun GameBanner(onClick: () -> Unit) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .height(IntrinsicSize.Max)
            .clickable(onClick = onClick)
    ) {
        Image(
            painter = painterResource(R.drawable.game_banner_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
                    .padding(vertical = 16.dp),
            ) {
                headline1_jacob(stringResource(R.string.SettingsBanner_Title))
                VSpacer(20.dp)
                Column(
                    modifier = Modifier.defaultMinSize(minHeight = 60.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.SettingsBanner_GameDescription),
                        style = ComposeAppTheme.typography.subhead,
                        color = Bright,
                    )
                    VSpacer(4.dp)
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
            )
        }
    }
}

@Preview
@Composable
fun PremiumBannerPreview() {
    ComposeAppTheme {
        GameBanner(
            onClick = {},
        )
    }
}