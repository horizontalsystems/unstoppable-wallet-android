package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
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
