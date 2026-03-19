package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.NavController
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet

class EditPinFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        PinSet(
            title = stringResource(R.string.EditPin_Title),
            description = stringResource(R.string.EditPin_NewPinInfo),
            dismissWithSuccess = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() }
        )
    }
}
