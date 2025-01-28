package io.horizontalsystems.bankwallet.modules.settings.donate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryDefault
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.bankwallet.ui.compose.components.subhead2_grey

class DonateTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Settings_DonateWith),
            onClickItem = { viewItem ->
                val donateAddress: String? = App.appConfigProvider.donateAddresses[viewItem.wallet.token.blockchainType]
                donateAddress?.let {
                    val sendTitle = Translator.getString(R.string.Settings_DonateToken, viewItem.wallet.token.fullCoin.coin.code)
                    navController.slideFromRight(
                        R.id.sendXFragment,
                        SendFragment.Input(
                            wallet = viewItem.wallet,
                            title = sendTitle,
                            sendEntryPointDestId = R.id.sendTokenSelectFragment,
                            address = Address(donateAddress),
                            hideAddress = true
                        )
                    )

                    stat(page = StatPage.Donate, event = StatEvent.OpenSend(viewItem.wallet.token))
                }

            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend)
        ) { DonateHeader(navController) }
    }
}

@Composable
private fun DonateHeader(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        VSpacer(24.dp)
        headline2_leah(
            text = stringResource(R.string.Settings_Donate_Info),
            textAlign = TextAlign.Center
        )
        VSpacer(24.dp)
        Icon(
            painter = painterResource(id = R.drawable.ic_heart_filled_24),
            tint = ComposeAppTheme.colors.jacob,
            contentDescription = null,
        )
    }

    GetAddressCell {
        navController.slideFromRight(R.id.donateAddressesFragment)

        stat(page = StatPage.Donate, event = StatEvent.Open(StatPage.DonateAddressList))
    }
}

@Composable
private fun GetAddressCell(
    onClick: () -> Unit
) {
    VSpacer(24.dp)
    ButtonPrimaryDefault(
        title = stringResource(R.string.Settings_Donate_GetAddress),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        onClick = onClick
    )
    VSpacer(24.dp)
    subhead2_grey(
        text = stringResource(R.string.Settings_Donate_OrSelectCoinToDonate),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    VSpacer(24.dp)
}

@Preview
@Composable
private fun DonateHeaderPreview() {
    ComposeAppTheme {
        DonateHeader(navController = rememberNavController())
    }
}