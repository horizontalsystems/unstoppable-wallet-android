package cash.p.terminal.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import io.horizontalsystems.core.setNavigationResultX
import cash.p.terminal.modules.pin.ui.PinConfirm
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.ui_compose.getInput
import kotlinx.parcelize.Parcelize

class ConfirmPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<InputConfirm>()

        PinConfirm(
            title = stringResource(input?.descriptionResId ?: R.string.Unlock_EnterPasscode),
            pinType = input?.pinType ?: PinType.REGULAR,
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
    data class InputConfirm(val descriptionResId: Int, val pinType: PinType) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
