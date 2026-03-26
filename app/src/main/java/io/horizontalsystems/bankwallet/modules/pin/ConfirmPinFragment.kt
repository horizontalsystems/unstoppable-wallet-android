package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
import kotlinx.parcelize.Parcelize

class ConfirmPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val resultEventBus = LocalResultEventBus.current
        PinConfirm(
            onSuccess = {
                resultEventBus.sendResult(Result(true))
                navController.removeLastOrNull()
            },
            onCancel = {
                navController.removeLastOrNull()
            }
        )
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
