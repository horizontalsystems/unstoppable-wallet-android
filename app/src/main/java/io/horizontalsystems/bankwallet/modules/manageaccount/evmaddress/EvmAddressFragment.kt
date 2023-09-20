package io.horizontalsystems.bankwallet.modules.manageaccount.evmaddress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.AppBar
import io.horizontalsystems.bankwallet.ui.compose.components.HsBackButton
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper

class EvmAddressFragment : BaseComposeFragment(screenshotEnabled = false) {

    companion object {
        const val EVM_ADDRESS_KEY = "evm_address_key"
    }

    @Composable
    override fun GetContent() {
        val evmAddress = arguments?.getString(EVM_ADDRESS_KEY) ?: ""
        EvmAddressScreen(evmAddress, findNavController())
    }

}

@Composable
private fun EvmAddressScreen(evmAddress: String, navController: NavController) {
    val view = LocalView.current
    ComposeAppTheme {
        Column(modifier = Modifier.background(color = ComposeAppTheme.colors.tyler)) {
            AppBar(
                title = stringResource(R.string.PublicKeys_EvmAddress),
                navigationIcon = {
                    HsBackButton(onClick = navController::popBackStack)
                },
                menuItems = listOf(
                    MenuItem(
                        title = TranslatableString.ResString(R.string.Info_Title),
                        icon = R.drawable.ic_info_24,
                        onClick = {
                            FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)
                        }
                    )
                )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(12.dp))
                HidableContent(evmAddress)
                Spacer(Modifier.height(24.dp))
            }
            ActionButton(R.string.Alert_Copy) {
                TextHelper.copyText(evmAddress)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)
            }
        }
    }
}