package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.serialization.Serializable

@Serializable
data object EditDuressPinPage : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        PinSet(
            title = stringResource(id = R.string.EditDuressPin_Title),
            description = stringResource(id = R.string.EditDuressPin_Description),
            dismissWithSuccess = { navController.removeLastOrNull() },
            onBackPress = { navController.removeLastOrNull() },
            forDuress = true
        )
    }
}
