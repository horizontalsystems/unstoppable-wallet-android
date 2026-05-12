package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.serialization.Serializable

@Serializable
data object EditPinFragment : HSScreen(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        PinSet(
            title = stringResource(R.string.EditPin_Title),
            description = stringResource(R.string.EditPin_NewPinInfo),
            dismissWithSuccess = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() }
        )
    }
}
