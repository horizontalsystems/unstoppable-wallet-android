package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R.string.Button_Apply
import io.horizontalsystems.bankwallet.R.string.SendEvmSettings_SetRecipient
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapSettingsRecipientPage(val input: Input) : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        EnterAddressScreen(
            navController = navController,
            token = input.token,
            title = stringResource(SendEvmSettings_SetRecipient),
            buttonTitle = stringResource(Button_Apply),
            allowNull = true,
            initialAddress = input.recipient?.hex
        ) { address, _ ->
            resultEventBus.sendResult<Result>(Result(address))
            navController.removeLastOrNull()
        }
    }

    @Serializable
    data class Input(val token: Token, val recipient: Address?)

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}
