package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.modules.pin.SetPinFragment.Input
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinSet
import io.horizontalsystems.core.getInput

class EditPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()

        PinSet(
            title = stringResource(R.string.EditPin_Title),
            description = stringResource(R.string.EditPin_NewPinInfo),
            pinType = input?.pinType ?: PinType.REGULAR,
            dismissWithSuccess = { navController.popBackStack() },
            onBackPress = { navController.popBackStack() }
        )
    }
}
