package com.quantum.wallet.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.quantum.wallet.bankwallet.R.string.Button_Apply
import com.quantum.wallet.bankwallet.R.string.SendEvmSettings_SetRecipient
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.core.setNavigationResultX
import com.quantum.wallet.bankwallet.entities.Address
import com.quantum.wallet.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize

class SwapSettingsRecipientFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
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
                navController.popBackStack()
            }
        }
    }

    @Parcelize
    data class Input(val token: Token, val recipient: Address?) : Parcelable

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}
