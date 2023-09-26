package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class ConfirmPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
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
