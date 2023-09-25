package io.horizontalsystems.bankwallet.modules.pin

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.HudHelper
import io.horizontalsystems.core.parcelable
import kotlinx.parcelize.Parcelize

class SetDuressPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val viewModel = viewModel<SetDuressPinViewModel>(factory = SetDuressPinViewModel.Factory(arguments?.parcelable("input")))
        val view = LocalView.current
        ComposeAppTheme {
            PinSet(
                title = stringResource(id = R.string.SetDuressPin_Title),
                description = stringResource(id = R.string.SetDuressPin_Description),
                dismissWithSuccess = {
                    viewModel.onDuressPinSet()
                    HudHelper.showSuccessMessage(view, R.string.Hud_Text_Created)
                    findNavController().popBackStack(R.id.setDuressPinIntroFragment, true)
                },
                onBackPress = { findNavController().popBackStack() },
                forDuress = true
            )
        }
    }

    companion object {
        fun params(accountIds: List<String>): Bundle {
            return bundleOf("input" to Input(accountIds))
        }
    }

    @Parcelize
    data class Input(val accountIds: List<String>) : Parcelable
}
