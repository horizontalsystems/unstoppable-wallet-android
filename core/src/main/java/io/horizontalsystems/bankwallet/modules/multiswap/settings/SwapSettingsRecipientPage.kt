package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.core.R.string.Button_Apply
import io.horizontalsystems.core.R.string.SendEvmSettings_SetRecipient
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
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        EnterAddressScreen(
            navigation = navigation,
            token = input.token,
            title = stringResource(SendEvmSettings_SetRecipient),
            buttonTitle = stringResource(Button_Apply),
            allowNull = true,
            initialAddress = input.recipient?.hex
        ) { address, _ ->
            resultEventBus.sendResult<Result>(Result(address))
            navigation.removeLastOrNull()
        }
    }

    @Serializable
    data class Input(val token: Token, val recipient: Address?)

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}
