package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
import kotlinx.parcelize.Parcelize
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

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
