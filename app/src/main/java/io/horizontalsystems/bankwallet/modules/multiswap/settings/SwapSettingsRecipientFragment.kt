package io.horizontalsystems.bankwallet.modules.multiswap.settings

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.serializers.TokenSerializer
import io.horizontalsystems.marketkit.models.Token
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SwapSettingsRecipientScreen(
    @Serializable(with = TokenSerializer::class)
    val token: Token,
    val recipient: Address?
) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val resultBus = LocalResultEventBus.current
        EnterAddressScreen(
            backStack = backStack,
            token = token,
            title = stringResource(R.string.SendEvmSettings_SetRecipient),
            buttonTitle = stringResource(R.string.Button_Apply),
            allowNull = true,
            initialAddress = recipient?.hex
        ) { address, _ ->
            resultBus.sendResult(result = Result(address))
            backStack.removeLastOrNull()
        }
    }

    data class Result(val address: Address?)
}

class SwapSettingsRecipientFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(val token: Token, val recipient: Address?) : Parcelable

    @Parcelize
    data class Result(val address: Address?) : Parcelable
}
