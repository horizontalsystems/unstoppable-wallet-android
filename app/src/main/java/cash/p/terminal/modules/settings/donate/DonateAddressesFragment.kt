package cash.p.terminal.modules.settings.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.ui_compose.components.AppBar
import cash.p.terminal.ui_compose.components.ButtonSecondaryCircle
import cash.p.terminal.ui_compose.components.CellUniversalLawrenceSection
import cash.p.terminal.ui_compose.components.HSpacer
import cash.p.terminal.ui_compose.components.HsBackButton
import cash.p.terminal.ui.compose.components.InfoText
import cash.p.terminal.ui_compose.components.RowUniversal
import cash.p.terminal.ui_compose.components.VSpacer
import cash.p.terminal.ui_compose.components.subhead2_leah
import cash.p.terminal.ui.helpers.TextHelper
import cash.p.terminal.ui_compose.theme.ComposeAppTheme
import cash.p.terminal.wallet.title
import io.horizontalsystems.chartview.rememberAsyncImagePainterWithFallback
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.imageUrl

class DonateAddressesFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        DonateScreen(
            onBackPress = { navController.popBackStack() }
        )
    }

}

@Composable
fun DonateScreen(
    onBackPress: () -> Unit
) {
    Scaffold(
        containerColor = ComposeAppTheme.colors.tyler,
        topBar = {
            AppBar(
                title = stringResource(R.string.Settings_Donate_Addresses),
                navigationIcon = {
                    HsBackButton(onClick = onBackPress)
                },
            )
        }
    ) {
        Column(Modifier.padding(it)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                VSpacer(12.dp)
                App.appConfigProvider.donateAddresses.forEach { (blockchainType, address) ->
                    DonateAddress(
                        coinImageUrl = blockchainType.imageUrl,
                        coinName = blockchainType.title,
                        address = address,
                        chainUid = blockchainType.uid
                    )
                    VSpacer(24.dp)
                }

                VSpacer(8.dp)
            }
        }
    }
}

@Composable
private fun DonateAddress(
    coinImageUrl: String,
    coinName: String,
    address: String,
    chainUid: String
) {
    val localView = LocalView.current

    InfoText(text = coinName.uppercase())
    CellUniversalLawrenceSection() {
        RowUniversal(
            modifier = Modifier.padding(horizontal = 16.dp),
            onClick = {
                TextHelper.copyText(address)
                HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)

                stat(page = StatPage.DonateAddressList, event = StatEvent.CopyAddress(chainUid))
            }
        ) {
            Image(
                modifier = Modifier.size(32.dp),
                painter = rememberAsyncImagePainterWithFallback(
                    model = coinImageUrl,
                    error = painterResource(R.drawable.ic_platform_placeholder_32)
                ),
                contentDescription = "platform"
            )
            HSpacer(16.dp)
            subhead2_leah(
                modifier = Modifier.weight(1f),
                text = address,
            )

            HSpacer(16.dp)
            ButtonSecondaryCircle(
                icon = R.drawable.ic_copy_20,
                contentDescription = stringResource(R.string.Button_Copy),
                onClick = {
                    TextHelper.copyText(address)
                    HudHelper.showSuccessMessage(localView, R.string.Hud_Text_Copied)

                    stat(page = StatPage.DonateAddressList, event = StatEvent.CopyAddress(chainUid))
                }
            )
        }
    }
}

@Preview
@Composable
fun DonateScreenPreview() {
    ComposeAppTheme {
        DonateScreen {}
    }
}