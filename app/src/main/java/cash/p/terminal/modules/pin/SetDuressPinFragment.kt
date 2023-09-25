package cash.p.terminal.modules.pin

import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinSet
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class SetDuressPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinSet(
                title = "Set Duress Passcode",
                description = "Duress Passcode",
                dismissWithSuccess = { findNavController().popBackStack() },
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
