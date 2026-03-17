package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object ConfirmPinScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val resultBus = LocalResultEventBus.current
        PinConfirm(
            onSuccess = {
                resultBus.sendResult(result = Result(true))
                backStack.removeLastOrNull()
            },
            onCancel = {
                backStack.removeLastOrNull()
            }
        )
    }

    data class Result(val success: Boolean)
}

class ConfirmPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
//        PinConfirm(
//            onSuccess = {
//                navController.setNavigationResultX(Result(true))
//                navController.popBackStack()
//            },
//            onCancel = {
//                navController.popBackStack()
//            }
//        )
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
