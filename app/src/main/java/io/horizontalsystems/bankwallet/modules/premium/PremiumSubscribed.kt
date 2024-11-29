package io.horizontalsystems.bankwallet.modules.premium

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.Darkest
import io.horizontalsystems.bankwallet.ui.compose.SteelLight
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer

@Composable
fun PremiumSubscribedScreen(
    type: PremiumPlanType,
    onCloseClick: () -> Unit
) {
    Scaffold(
        backgroundColor = Darkest,
        topBar = {
            TitleCenteredTopBar(
                modifier = Modifier.padding(top = 24.dp),
                title = stringResource(R.string.Premium_Title),
                color = SteelLight,
                onCloseClick = onCloseClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .paint(
                    painterResource(id = R.drawable.prem_backround),
                    contentScale = ContentScale.FillBounds
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            )
            {
                VSpacer(24.dp)
                Image(
                    painter = painterResource(id = R.drawable.prem_star_launch),
                    contentDescription = null,
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth()
                )
                VSpacer(24.dp)
                Text(
                    text = stringResource(R.string.Premium_ThankYouForSubscription),
                    style = ComposeAppTheme.typography.headline1,
                    color = ComposeAppTheme.colors.white,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 44.dp)
                )
                val text = highlightText(
                    text = stringResource(
                        R.string.Premium_YouHaveActivatedPlan,
                        stringResource(type.titleResId)
                    ),
                    highlightPart = stringResource(type.titleResId),
                    color = ComposeAppTheme.colors.jacob
                )
                VSpacer(12.dp)
                Text(
                    text = text,
                    style = ComposeAppTheme.typography.body,
                    color = ComposeAppTheme.colors.grey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
                VSpacer(24.dp)
            }
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 56.dp)
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