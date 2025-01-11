package cash.p.terminal.modules.subscription

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.slideFromBottom
import cash.p.terminal.modules.evmfee.ButtonsGroupWithShade
import cash.p.terminal.modules.info.ui.InfoHeader
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonPrimaryTransparent
import cash.p.terminal.ui_compose.components.ButtonPrimaryYellow
import cash.p.terminal.ui.compose.components.InfoH3
import cash.p.terminal.ui_compose.components.MenuItem
import cash.p.terminal.ui_compose.components.body_bran
import cash.p.terminal.ui_compose.theme.ComposeAppTheme

class SubscriptionInfoFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val uriHandler = LocalUriHandler.current

        SubscriptionInfoScreen(
            onClickGetPremium = {
                uriHandler.openUri(App.appConfigProvider.analyticsLink)
            },
            onClickHavePremium = {
                navController.popBackStack()
                navController.slideFromBottom(R.id.activateSubscription)
            },
            onClose = {
                navController.popBackStack()
            }
        )
    }

}

@Composable
private fun SubscriptionInfoScreen(
    onClickGetPremium: () -> Unit,
    onClickHavePremium: () -> Unit,
    onClose: () -> Unit
) {
    Surface(color = ComposeAppTheme.colors.tyler) {
        Column {
            AppBar(
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Button_Close),
                        icon = R.drawable.ic_close,
                        onClick = onClose
                    )
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoHeader(R.string.SubscriptionInfo_Title)

                InfoH3(stringResource(R.string.SubscriptionInfo_Analytics_Title))
                body_bran(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                    text = stringResource(R.string.SubscriptionInfo_Analytics_Info)
                )

                InfoH3(stringResource(R.string.SubscriptionInfo_Indicators_Title))
                body_bran(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                    text = stringResource(R.string.SubscriptionInfo_Indicators_Info)
                )

                InfoH3(stringResource(R.string.SubscriptionInfo_PersonalSupport_Title))
                body_bran(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
                    text = stringResource(R.string.SubscriptionInfo_PersonalSupport_Info)
                )
            }

            ButtonsGroupWithShade {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    ButtonPrimaryYellow(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.SubscriptionInfo_GetPremium),
                        onClick = onClickGetPremium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ButtonPrimaryTransparent(
                        modifier = Modifier.fillMaxWidth(),
                        title = stringResource(R.string.SubscriptionInfo_HavePremium),
                        onClick = onClickHavePremium
                    )
                }
            }
        }
    }
}
