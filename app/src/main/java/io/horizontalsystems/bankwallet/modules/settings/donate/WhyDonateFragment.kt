package io.horizontalsystems.bankwallet.modules.settings.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellowWithIcon
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.RadialBackground
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_jacob
import io.horizontalsystems.bankwallet.ui.compose.components.headline1_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subheadR_leah
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold

class WhyDonateFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        WhyDonateView(
            onClick = {
                navController.slideFromRight(R.id.donateTokenSelectFragment)
                stat(page = StatPage.Settings, event = StatEvent.Open(StatPage.Donate))
            },
            onClose = {
                navController.popBackStack()
            }
        )
    }

}

@Composable
fun WhyDonateView(
    onClick: () -> Unit,
    onClose: () -> Unit
) {

    HSScaffold(
        title = "",
        onBack = onClose,
    ) {
        Box(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize()
        ) {
            RadialBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.SettingsBanner_DonateTitle),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 30.sp,
                    color = ComposeAppTheme.colors.leah,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                VSpacer(12.dp)
                body_jacob(
                    text = stringResource(R.string.SettingsBanner_DonateSubtitle),
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                VSpacer(36.dp)
                ButtonPrimaryYellowWithIcon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    icon = R.drawable.ic_heart_filled_24,
                    iconTint = Color.Black,
                    title = stringResource(R.string.SettingsBanner_SupportTheProject),
                    onClick = onClick
                )
                VSpacer(12.dp)
                Image(
                    painter = painterResource(id = R.drawable.donate_pic1),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(12.dp)
                headline1_leah(
                    text = stringResource(R.string.SettingsBanner_DonateUnstoppableHasBeenFreeFromDayOne),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
                VSpacer(12.dp)
                body_jacob(
                    text = stringResource(R.string.SettingsBanner_DonateUnstoppableWalletTeam),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                VSpacer(12.dp)
                Image(
                    painter = painterResource(id = R.drawable.donate_pic2),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(12.dp)
                headline1_leah(
                    text = stringResource(R.string.SettingsBanner_DonateByJoiningMovement),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                VSpacer(24.dp)
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateYourSupportIsCommitment),
                    )
                    HSpacer(12.dp)
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateItHelpsWalletToBeAdFree),
                    )
                }
                VSpacer(12.dp)
                Image(
                    painter = painterResource(id = R.drawable.donate_pic3),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
                body_jacob(
                    text = stringResource(R.string.SettingsBanner_DonateExclusivelyOnFDroid),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                VSpacer(12.dp)
                headline1_leah(
                    text = stringResource(R.string.SettingsBanner_DonateUnstoppableWalletGivesYouAdvancedFeatures),
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                VSpacer(24.dp)
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeatureFullAnalytics),
                    )
                    HSpacer(12.dp)
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeaturePrivacy),
                    )
                }
                VSpacer(24.dp)
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeatureSignals),
                    )
                    HSpacer(12.dp)
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeatureAddress),
                    )
                }
                VSpacer(24.dp)
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeatureAdvanced),
                    )
                    HSpacer(12.dp)
                    subheadR_leah(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.SettingsBanner_DonateFeaturePriority),
                    )
                }
                VSpacer(12.dp)
                Image(
                    painter = painterResource(id = R.drawable.donate_pic4),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth()
                )
                VSpacer(12.dp)
                body_jacob(
                    text = stringResource(R.string.SettingsBanner_DonateEveryBitOfSupportMatters),
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                VSpacer(12.dp)
                headline1_leah(
                    text = stringResource(R.string.SettingsBanner_DonateBySupportingUnstoppableWalletYouHelpGrow),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center
                )
                VSpacer(36.dp)
                ButtonPrimaryYellowWithIcon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    icon = R.drawable.ic_heart_filled_24,
                    iconTint = Color.Black,
                    title = stringResource(R.string.SettingsBanner_SupportTheProject),
                    onClick = onClick
                )
                VSpacer(32.dp)
            }
        }
    }
}

@Preview
@Composable
private fun Preview_SelectSubscriptionScreen() {
    ComposeAppTheme {
        WhyDonateView(
            {},
            {}
        )
    }
}