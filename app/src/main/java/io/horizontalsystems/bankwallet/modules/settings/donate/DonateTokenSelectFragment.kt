package io.horizontalsystems.bankwallet.modules.settings.donate

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Divider
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
import io.horizontalsystems.bankwallet.modules.send.SendFragment
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectScreen
import io.horizontalsystems.bankwallet.modules.tokenselect.TokenSelectViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.RowUniversal
import io.horizontalsystems.bankwallet.ui.compose.components.VSpacer
import io.horizontalsystems.bankwallet.ui.compose.components.body_leah
import io.horizontalsystems.bankwallet.ui.compose.components.headline2_leah
import io.horizontalsystems.core.findNavController

class DonateTokenSelectFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
        TokenSelectScreen(
            navController = navController,
            title = stringResource(R.string.Settings_DonateWith),
            onClickItem = {
                val donateAddress: String? = App.appConfigProvider.donateAddresses[it.wallet.token.blockchainType]
                val sendTitle = Translator.getString(R.string.Settings_DonateToken, it.wallet.token.fullCoin.coin.code)
                navController.slideFromRight(
                    R.id.sendXFragment,
                    SendFragment.prepareParams(
                        it.wallet,
                        R.id.sendTokenSelectFragment,
                        sendTitle,
                        donateAddress,
                    )
                )
            },
            viewModel = viewModel(factory = TokenSelectViewModel.FactoryForSend()),
            emptyItemsText = stringResource(R.string.Balance_NoAssetsToSend),
            header = { DonateHeader(navController) }
        )
    }
}

@Composable
private fun DonateHeader(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        headline2_leah(
            text = stringResource(R.string.Settings_Donate_Info),
            textAlign = TextAlign.Center
        )
        VSpacer(24.dp)
        Image(
            painter = painterResource(id = R.drawable.hands_32),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .padding(horizontal = 24.dp)
        )
    }

    GetAddressCell {
        navController.slideFromRight(R.id.donateAddressesFragment)
    }
}

@Composable
private fun GetAddressCell(
    onClick: () -> Unit
) {
    Divider(
        thickness = 1.dp,
        color = ComposeAppTheme.colors.steel10,
    )
    RowUniversal(
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = onClick
    ) {
        body_leah(
            text = stringResource(R.string.Settings_Donate_GetAddress),
            maxLines = 1,
            modifier = Modifier
                .padding(end = 16.dp)
                .weight(1f)
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
private fun DonateHeaderPreview() {
    ComposeAppTheme {
        DonateHeader(navController = rememberNavController())
    }
}