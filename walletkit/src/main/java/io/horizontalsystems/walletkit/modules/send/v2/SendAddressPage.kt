package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.entities.Address
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.enteraddress.EnterAddressScreen
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import kotlinx.serialization.Serializable

/** Recipient entry for [SendV2Page]; returns the validated address as [Result]. */
@Serializable
data class SendAddressPage(val wallet: Wallet, val address: String? = null) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current

        EnterAddressScreen(
            navigation = navigation,
            token = wallet.token,
            title = stringResource(R.string.Send_EnterAddress),
            buttonTitle = stringResource(R.string.Button_Next),
            allowNull = false,
            initialAddress = address,
        ) { result, risky ->
            result?.let {
                resultEventBus.sendResult(Result(it, risky))
                navigation.removeLastOrNull()
            }
        }
    }

    data class Result(val address: Address, val risky: Boolean)
}
