package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SetPinScreen(val descriptionResId: Int? = null) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        PinSet(
            title = stringResource(R.string.PinSet_Title),
            description = stringResource(descriptionResId ?: R.string.PinSet_Info),
            dismissWithSuccess = {
                resultBus.sendResult(result = Result(true))
                backStack.removeLastOrNull()
            },
            onBackPress = { backStack.removeLastOrNull() }
        )
    }

    data class Result(val success: Boolean)
}

class SetPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
//        val input = navController.getInput<Input>()
//
//        PinSet(
//            title = stringResource(R.string.PinSet_Title),
//            description = stringResource(input?.descriptionResId ?: R.string.PinSet_Info),
//            dismissWithSuccess = {
//                navController.setNavigationResultX(Result(true))
//                navController.popBackStack()
//            },
//            onBackPress = { navController.popBackStack() }
//        )
    }

    @Parcelize
    data class Input(val descriptionResId: Int) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
