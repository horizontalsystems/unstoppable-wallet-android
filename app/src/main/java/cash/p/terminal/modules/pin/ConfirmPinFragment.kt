package cash.p.terminal.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.pin.ui.PinConfirm
import cash.p.terminal.ui.compose.ComposeAppTheme
import kotlinx.parcelize.Parcelize

class ConfirmPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ComposeAppTheme {
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
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
