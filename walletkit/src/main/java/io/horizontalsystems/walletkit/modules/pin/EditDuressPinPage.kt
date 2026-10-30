package io.horizontalsystems.walletkit.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.pin.ui.PinSet
import kotlinx.serialization.Serializable

@Serializable
data object EditDuressPinPage : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val view = LocalView.current
        PinSet(
            title = stringResource(id = R.string.EditDuressPin_Title),
            description = stringResource(id = R.string.EditDuressPin_Description),
            dismissWithSuccess = {
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Changed)
                navigation.removeLastOrNull()
            },
            onBackPress = { navigation.removeLastOrNull() },
            forDuress = true
        )
    }
}
