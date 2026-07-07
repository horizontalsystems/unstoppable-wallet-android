package io.horizontalsystems.walletkit.modules.pin

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.modules.pin.ui.PinConfirm
import kotlinx.serialization.Serializable

@Serializable
data object ConfirmPinPage : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        PinConfirm(
            onSuccess = {
                resultEventBus.sendResult(Result(true))
                navigation.removeLastOrNull()
            },
            onCancel = {
                navigation.removeLastOrNull()
            }
        )
    }

    data class Result(val success: Boolean)
}
