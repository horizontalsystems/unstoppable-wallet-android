package cash.p.terminal.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.pin.ui.PinConfirm
import kotlinx.parcelize.Parcelize

class ConfirmPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        PinConfirm(
            onSuccess = {
                navController.setNavigationResultX(Result(true))
                navController.popBackStack()
            },
            onCancel = {
                navController.popBackStack()
            }
        )
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
