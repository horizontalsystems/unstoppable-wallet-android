package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.serialization.Serializable

@Serializable
data object EditDuressPinFragment : HSScreen(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        PinSet(
            title = stringResource(id = R.string.EditDuressPin_Title),
            description = stringResource(id = R.string.EditDuressPin_Description),
            dismissWithSuccess = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() },
            forDuress = true
        )
    }
}
