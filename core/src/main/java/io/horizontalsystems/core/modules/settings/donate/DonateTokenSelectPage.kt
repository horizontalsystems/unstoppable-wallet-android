package io.horizontalsystems.core.modules.settings.donate

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.core.R
import io.horizontalsystems.core.core.App
import io.horizontalsystems.core.core.providers.Translator
import io.horizontalsystems.core.core.stats.StatEvent
import io.horizontalsystems.core.core.stats.StatPage
import io.horizontalsystems.core.core.stats.stat
import io.horizontalsystems.core.entities.Address
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.send.SendPage
import io.horizontalsystems.core.modules.sendtokenselect.SendTokenSelectPage
import io.horizontalsystems.core.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.core.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.core.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.uiv3.components.cell.CellMiddleInfo
import io.horizontalsystems.core.uiv3.components.cell.CellPrimary
import io.horizontalsystems.core.uiv3.components.cell.CellRightNavigation
import io.horizontalsystems.core.uiv3.components.cell.hs
import kotlinx.serialization.Serializable

@Serializable
data object DonateTokenSelectPage : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        TokenSelectScreen(
            navigation = navigation,
            title = stringResource(R.string.Settings_Donate),
            onClickItem = { viewItem ->
                val donateAddress: String? =
                    App.appConfigProvider.donateAddresses[viewItem.wallet.token.blockchainType]
                donateAddress?.let {
                    val sendTitle = Translator.getString(
                        R.string.Settings_DonateToken,
                        viewItem.wallet.token.fullCoin.coin.code
                    )
                    navigation.slideFromRight(
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
                    navigation.slideFromRight(DonateAddressesPage)

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