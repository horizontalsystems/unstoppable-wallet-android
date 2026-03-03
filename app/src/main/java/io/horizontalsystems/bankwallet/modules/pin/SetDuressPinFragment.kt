package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class SetDuressPinScreen(val accountIds: List<String>?) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        val viewModel = viewModel<SetDuressPinViewModel>(
            factory = SetDuressPinViewModel.Factory(accountIds)
        )
        val view = LocalView.current
        PinSet(
            title = stringResource(id = R.string.SetDuressPin_Title),
            description = stringResource(id = R.string.SetDuressPin_Description),
            dismissWithSuccess = {
                viewModel.onDuressPinSet()
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
//                TODO("xxx nav3")
//                navController.popBackStack(R.id.setDuressPinIntroFragment, true)
            },
            onBackPress = { backStack.removeLastOrNull() },
            forDuress = true
        )
    }
}

class SetDuressPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
//        val viewModel = viewModel<SetDuressPinViewModel>(
//            factory = SetDuressPinViewModel.Factory(navController.getInput())
//        )
//        val view = LocalView.current
//        PinSet(
//            title = stringResource(id = R.string.SetDuressPin_Title),
//            description = stringResource(id = R.string.SetDuressPin_Description),
//            dismissWithSuccess = {
//                viewModel.onDuressPinSet()
//                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
//                navController.popBackStack(R.id.setDuressPinIntroFragment, true)
//            },
//            onBackPress = { navController.popBackStack() },
//            forDuress = true
//        )
    }

    @Parcelize
    data class Input(val accountIds: List<String>) : Parcelable
}
