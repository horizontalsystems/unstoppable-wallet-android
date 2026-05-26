package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.serialization.Serializable

@Serializable
data class SetDuressPinPage(val input: Input? = null) : HSPage(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = hiltViewModel<SetDuressPinViewModel, SetDuressPinViewModel.Factory> { factory ->
            factory.create(input)
        }
        val view = LocalView.current
        PinSet(
            title = stringResource(id = R.string.SetDuressPin_Title),
            description = stringResource(id = R.string.SetDuressPin_Description),
            dismissWithSuccess = {
                viewModel.onDuressPinSet()
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
                navController.removeLastUntil(SetDuressPinIntroPage::class, true)
            },
            onBackPress = { navController.removeLastOrNull() },
            forDuress = true
        )
    }

    @Serializable
    data class Input(val accountIds: List<String>)
}
