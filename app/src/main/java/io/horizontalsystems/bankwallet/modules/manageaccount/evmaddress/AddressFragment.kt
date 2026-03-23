package io.horizontalsystems.bankwallet.modules.manageaccount.evmaddress

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
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.managers.FaqManager
import io.horizontalsystems.bankwallet.core.stats.StatEntity
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.ActionButton
import io.horizontalsystems.bankwallet.modules.manageaccount.ui.HidableContent
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.components.MenuItem
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.bankwallet.uiv3.components.HSScaffold
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class AddressFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
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
    navController: NavBackStack<HSScreen>,
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
        onBack = { navController.removeLastOrNull() },
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