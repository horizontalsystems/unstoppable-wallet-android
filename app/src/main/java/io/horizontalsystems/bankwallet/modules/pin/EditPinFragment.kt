package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

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
