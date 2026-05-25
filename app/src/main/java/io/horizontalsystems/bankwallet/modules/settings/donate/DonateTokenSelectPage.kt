package io.horizontalsystems.bankwallet.modules.settings.donate

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.send.SendPage
import io.horizontalsystems.bankwallet.modules.sendtokenselect.SendTokenSelectPage
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellPrimary
import io.horizontalsystems.bankwallet.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.bankwallet.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

@Serializable
data object DonateTokenSelectPage : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Settings_Donate),
            onClickItem = { viewItem ->
                val donateAddress: String? =
                    App.appConfigProvider.donateAddresses[viewItem.wallet.token.blockchainType]
                donateAddress?.let {
                    val sendTitle = Translator.getString(
                        R.string.Settings_DonateToken,
                        viewItem.wallet.token.fullCoin.coin.code
                    )
                    navController.slideFromRight(
                        SendPage(SendPage.Input(
                            wallet = viewItem.wallet,
                            title = sendTitle,
                            sendEntryPointDestId = SendTokenSelectPage::class,
                            address = Address(donateAddress),
                            hideAddress = true
                        ))
                    )

                    stat(page = StatPage.Donate, event = StatEvent.OpenSend(viewItem.wallet.token))
                }

            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
        ) {
            DonateHeader(
                onClick = {
                    navController.slideFromRight(DonateAddressesPage)

                    stat(page = StatPage.Donate, event = StatEvent.Open(StatPage.DonateAddressList))
                }
            )
        }
    }
}

@Composable
private fun DonateHeader(onClick: () -> Unit) {
    CellPrimary(
        middle = {
            CellMiddleInfo(title = stringResource(R.string.Settings_Donate_DonationAddresses).hs)
        },
        right = {
            CellRightNavigation()
        },
        onClick = onClick
    )
}

@Preview
@Composable
private fun DonateHeaderPreview() {
    ComposeAppTheme {
        DonateHeader({})
    }
}