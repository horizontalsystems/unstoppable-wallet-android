package com.quantum.wallet.bankwallet.modules.settings.donate

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.providers.Translator
import com.quantum.wallet.bankwallet.core.slideFromRight
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.modules.send.SendFragment
import com.quantum.wallet.bankwallet.modules.tokenselect.TokenSelectScreen
import com.quantum.wallet.bankwallet.modules.tokenselect.TokenSelectViewModel
import com.quantum.wallet.bankwallet.ui.compose.ComposeAppTheme
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellMiddleInfo
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellPrimary
import com.quantum.wallet.bankwallet.uiv3.components.cell.CellRightNavigation
import com.quantum.wallet.bankwallet.uiv3.components.cell.hs

class DonateTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
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
        ) {
            DonateHeader(
                onClick = {
                    navController.slideFromRight(R.id.donateAddressesFragment)

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