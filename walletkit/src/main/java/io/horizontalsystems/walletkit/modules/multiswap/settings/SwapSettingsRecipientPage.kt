package io.horizontalsystems.walletkit.modules.multiswap.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.R.string.Button_Apply
import io.horizontalsystems.walletkit.R.string.Button_Done
import io.horizontalsystems.walletkit.R.string.SplitSwap_Address
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
            title = input.splitIndex?.let { stringResource(SplitSwap_Address, it) }
                ?: stringResource(SendEvmSettings_SetRecipient),
            buttonTitle = stringResource(if (input.splitIndex != null) Button_Done else Button_Apply),
            allowNull = input.allowRemoval,
            initialAddress = input.recipient?.hex,
            allowOwnAddress = true,
            zcashTransparentOnly = input.zcashTransparentOnly,
            // A split's address page slides up from the Recipients screen, so it closes with a cross
            backIcon = if (input.splitIndex != null) R.drawable.close_24 else null,
        ) { address, _, _ ->
            resultEventBus.sendResult<Result>(Result(address))
            navigation.removeLastOrNull()
        }
    }

    @Serializable
    data class Input(
        val token: Token,
        val recipient: Address?,
        val allowRemoval: Boolean = true,
        // Which of a split's recipients is being entered, numbered from 1, for the page title
        val splitIndex: Int? = null,
        // the selected route delivers only to transparent Zcash addresses
        val zcashTransparentOnly: Boolean = false,
    )

    data class Result(val address: Address?)
}
