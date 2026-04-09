package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.core.helpers.HudHelper
import kotlinx.parcelize.Parcelize

class SetDuressPinFragment(val input: Input? = null) : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val viewModel = viewModel<SetDuressPinViewModel>(
            factory = SetDuressPinViewModel.Factory(input)
        )
        val view = LocalView.current
        PinSet(
            title = stringResource(id = R.string.SetDuressPin_Title),
            description = stringResource(id = R.string.SetDuressPin_Description),
            dismissWithSuccess = {
                viewModel.onDuressPinSet()
                HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
                navController.removeLastUntil(SetDuressPinIntroFragment::class, true)
            },
            onBackPress = { navController.removeLastOrNull() },
            forDuress = true
        )
    }

    @Parcelize
    data class Input(val accountIds: List<String>) : Parcelable
}
