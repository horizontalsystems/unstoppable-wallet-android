package io.horizontalsystems.bankwallet.modules.settings.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.core.title
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonSecondaryCircle
import io.horizontalsystems.bankwallet.ui.compose.components.CellUniversalLawrenceSection
import io.horizontalsystems.bankwallet.ui.compose.components.HSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.InfoText
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_leah
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper

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
    HSScaffold(
        title = stringResource(R.string.Settings_Donate_Addresses),
        onBack = onBackPress,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
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
                painter = rememberAsyncImagePainter(
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