package io.horizontalsystems.walletkit.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R.string.Button_Apply
import io.horizontalsystems.walletkit.R.string.SendEvmSettings_SetRecipient
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.marketkit.models.Token
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
            allowNull = input.allowRemoval,
            initialAddress = input.recipient?.hex,
            allowOwnAddress = true,
        ) { address, _ ->
            resultEventBus.sendResult<Result>(Result(address))
            navigation.removeLastOrNull()
        }
    }

    @Serializable
    data class Input(
        val token: Token,
        val recipient: Address?,
        val allowRemoval: Boolean = true,
    )

    data class Result(val address: Address?)
}
