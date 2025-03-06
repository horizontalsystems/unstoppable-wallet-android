package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah

@Composable
fun PremiumSubscribedScreen(
    onCloseClick: () -> Unit
) {
    Scaffold(
        backgroundColor = ComposeAppTheme.colors.tyler,
        topBar = {
            TitleCenteredTopBar(
                title = stringResource(R.string.Premium_Title),
                onCloseClick = onCloseClick
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            RadialBackground()
            Column {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VSpacer(24.dp)
                    Image(
                        painter = painterResource(id = R.drawable.prem_star_launch),
                        contentDescription = null,
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                    )
                    VSpacer(24.dp)
                    headline1_leah(
                        text = stringResource(R.string.Premium_ThankYouForSubscription),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 44.dp)
                    )
                    VSpacer(12.dp)
                    body_jacob(
                        text = stringResource(R.string.Premium_EnjoyFullPowerOfTheApp),
                        textAlign = TextAlign.Center,
                    )
                    VSpacer(24.dp)
                }
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 44.dp)
                ) {
                    ButtonPrimaryCustomColor(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.Premium_GoToApp),
                        brush = yellowGradient,
                        onClick = onCloseClick,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PremiumSubscribedScreenPreview() {
    ComposeAppTheme {
        PremiumSubscribedScreen(
            onCloseClick = {}
        )
    }
}