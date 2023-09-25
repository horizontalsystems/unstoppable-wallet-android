package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinSet
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class EditDuressPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinSet(
                title = stringResource(id = R.string.EditDuressPin_Title),
                description = stringResource(id = R.string.EditDuressPin_Description),
                dismissWithSuccess = { findNavController().popBackStack() },
                onBackPress = { findNavController().popBackStack() },
                forDuress = true
            )
        }
    }
}
