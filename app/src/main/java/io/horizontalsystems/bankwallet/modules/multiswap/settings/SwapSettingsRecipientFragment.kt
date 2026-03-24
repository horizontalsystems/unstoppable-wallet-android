package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R.string.Button_Apply
import io.horizontalsystems.bankwallet.R.string.SendEvmSettings_SetRecipient
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SwapSettingsRecipientFragment(input1: Input) : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        withInput<Input>(navController) { input ->
            EnterAddressScreen(
                navController = navController,
                token = input.token,
                title = stringResource(SendEvmSettings_SetRecipient),
                buttonTitle = stringResource(Button_Apply),
                allowNull = true,
                initialAddress = input.recipient?.hex
            ) { address, _ ->
                navController.setNavigationResultX<Result>(Result(address))
                navController.removeLastOrNull()
            }
        }
    }

    @Parcelize
    data class Input(val token: Token, val recipient: Address?) : Parcelable

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}
