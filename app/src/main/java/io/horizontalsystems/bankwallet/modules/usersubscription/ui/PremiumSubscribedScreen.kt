package io.horizontalsystems.bankwallet.modules.usersubscription.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah

@Composable
fun PremiumSubscribedScreen(
    type: PremiumPlanType,
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
                    headline1_leah(
                        text = stringResource(R.string.Premium_ThankYouForSubscription),
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
                        textColor = ComposeAppTheme.colors.leah,
                        highlightPart = stringResource(type.titleResId),
                        highlightColor = ComposeAppTheme.colors.jacob
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
                    if (type == PremiumPlanType.VipPlan) {
                        VSpacer(24.dp)
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ComposeAppTheme.colors.steel10),
                        ) {
                            VipItem(
                                icon = R.drawable.prem_vip_support_24,
                                title = R.string.Premium_UpgradeFeature_VipSupport,
                            ) {
                                //todo
                            }
                            Divider(color = ComposeAppTheme.colors.steel20)
                            VipItem(
                                icon = R.drawable.prem_chat_support_24,
                                title = R.string.Premium_UpgradeFeature_VipClub,
                            ) {
                                //todo
                            }
                        }
                    }
                    VSpacer(24.dp)
                }
                Column(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
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

@Composable
fun VipItem(
    icon: Int,
    title: Int,
    click: () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { click() }
            .background(ComposeAppTheme.colors.steel10)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            modifier = Modifier.size(24.dp),
            tint = ComposeAppTheme.colors.jacob,
            contentDescription = null
        )
        HSpacer(16.dp)
        body_leah(
            text = stringResource(title),
            modifier = Modifier.weight(1f)
        )
        Image(
            modifier = Modifier.size(20.dp),
            painter = painterResource(id = R.drawable.ic_arrow_right),
            contentDescription = null,
        )
    }
}

@Preview
@Composable
fun PremiumSubscribedScreenPreview() {
    ComposeAppTheme {
        PremiumSubscribedScreen(
            type = PremiumPlanType.ProPlan,
            onCloseClick = {}
        )
    }
}