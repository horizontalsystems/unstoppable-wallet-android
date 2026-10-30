package io.horizontalsystems.walletkit.modules.pin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.IPinComponent
import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.helpers.HudHelper
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.ui.compose.ComposeAppTheme
import io.horizontalsystems.walletkit.ui.extensions.HSBottomSheet
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetBody
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetHeaderV3
import io.horizontalsystems.walletkit.uiv3.components.bottomsheet.BottomSheetTextBlock
import io.horizontalsystems.walletkit.uiv3.components.controls.ButtonVariant
import io.horizontalsystems.walletkit.uiv3.components.controls.HSButton
import kotlinx.serialization.Serializable

@Serializable
data object DisableDuressPinSheet : HSBottomSheet() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        DisableDuressPinScreen(navigation)
    }
}

@Composable
private fun DisableDuressPinScreen(navigation: HSNavigation) {
    val viewModel = viewModel<DisableDuressPinViewModel>(factory = DisableDuressPinViewModel.Factory())
    val view = LocalView.current

    BottomSheetBody(
        buttons = {
            HSButton(
                title = stringResource(R.string.DisableDuressPin_Button),
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Secondary,
                onClick = {
                    viewModel.disableDuressPin()
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Disabled)
                    navigation.removeLastOrNull()
                }
            )
        },
        content = {
            BottomSheetHeaderV3(
                image72 = painterResource(R.drawable.trash_filled_24),
                imageTint = ComposeAppTheme.colors.lucian,
                title = stringResource(R.string.DisableDuressPin_Title)
            )
            BottomSheetTextBlock(stringResource(R.string.DisableDuressPin_Description))
        },
    )
}

class DisableDuressPinViewModel(private val pinComponent: IPinComponent) : ViewModel() {

    fun disableDuressPin() {
        pinComponent.disableDuressPin()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DisableDuressPinViewModel(App.pinComponent) as T
        }
    }
}
