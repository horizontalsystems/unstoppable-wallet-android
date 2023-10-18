package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class EditDuressPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ComposeAppTheme {
            PinSet(
                title = stringResource(id = R.string.EditDuressPin_Title),
                description = stringResource(id = R.string.EditDuressPin_Description),
                dismissWithSuccess = { navController.popBackStack() },
                onBackPress = { navController.popBackStack() },
                forDuress = true
            )
        }
    }
}
