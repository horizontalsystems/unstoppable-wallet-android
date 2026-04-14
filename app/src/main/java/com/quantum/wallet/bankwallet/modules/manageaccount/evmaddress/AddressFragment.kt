package com.quantum.wallet.bankwallet.modules.manageaccount.evmaddress

import android.os.Parcelable
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
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.managers.FaqManager
import com.quantum.wallet.bankwallet.core.stats.StatEntity
import com.quantum.wallet.bankwallet.core.stats.StatEvent
import com.quantum.wallet.bankwallet.core.stats.StatPage
import com.quantum.wallet.bankwallet.core.stats.stat
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.ActionButton
import com.quantum.wallet.bankwallet.modules.manageaccount.ui.HidableContent
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.MenuItem
import com.quantum.wallet.bankwallet.ui.helpers.TextHelper
import com.quantum.wallet.bankwallet.uiv3.components.HSScaffold
import com.quantum.wallet.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class AddressFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Input>(navController) { input ->
            AddressScreen(navController, input.address, input.type)
        }
    }

    @Parcelize
    data class Input(val address: String, val type: Type) : Parcelable

    @Parcelize
    enum class Type : Parcelable {
        Evm, Tron
    }
}

@Composable
private fun AddressScreen(
    navController: NavController,
    address: String,
    type: AddressFragment.Type
) {
    val view = LocalView.current

    val title = when (type) {
        AddressFragment.Type.Evm -> stringResource(R.string.PublicKeys_EvmAddress)
        AddressFragment.Type.Tron -> stringResource(R.string.PublicKeys_TronAddress)
    }

    val statPage = when (type) {
        AddressFragment.Type.Evm -> StatPage.EvmAddress
        AddressFragment.Type.Tron -> StatPage.TronAddress
    }

    val statEntity = when (type) {
        AddressFragment.Type.Evm -> StatEntity.EvmAddress
        AddressFragment.Type.Tron -> StatEntity.TronAddress
    }

    HSScaffold(
        title = title,
        onBack = { navController.popBackStack() },
        menuItems = listOf(
            MenuItem(
                title = TranslatableString.ResString(R.string.Info_Title),
                icon = R.drawable.ic_info_24,
                onClick = {
                    FaqManager.showFaqPage(navController, FaqManager.faqPathPrivateKeys)

                    stat(page = statPage, event = StatEvent.Open(StatPage.Info))
                }
            )
        )
    ) {
        Column {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(12.dp))
                HidableContent(address)
                Spacer(Modifier.height(24.dp))
            }
            ActionButton(R.string.Alert_Copy) {
                TextHelper.copyText(address)
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Copied)

                stat(page = statPage, event = StatEvent.Copy(statEntity))
            }
        }
    }
}