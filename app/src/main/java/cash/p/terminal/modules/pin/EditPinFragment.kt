package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinEdit
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class EditPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinEdit(
                dismissWithSuccess = { findNavController().popBackStack() },
                onBackPress = { findNavController().popBackStack() }
            )
        }
    }
}
