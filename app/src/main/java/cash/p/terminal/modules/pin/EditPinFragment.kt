package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinSet
import cash.p.terminal.ui.compose.ComposeAppTheme

class EditPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ComposeAppTheme {
            PinSet(
                title = stringResource(R.string.EditPin_Title),
                description = stringResource(R.string.EditPin_NewPinInfo),
                dismissWithSuccess = { navController.popBackStack() },
                onBackPress = { navController.popBackStack() }
            )
        }
    }
}
