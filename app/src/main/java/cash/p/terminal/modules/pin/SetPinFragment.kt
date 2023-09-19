package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinSet
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class SetPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinSet(
                dismissWithSuccess = ::dismissWithSuccess,
                onBackPress = { findNavController().popBackStack() }
            )
        }
    }

    private fun dismissWithSuccess() {
        val bundle = bundleOf(requestResult to PinModule.RESULT_OK)
        setNavigationResult(requestKey, bundle)
        findNavController().popBackStack()
    }

    companion object {
        const val requestKey = "setPinRequestKey"
        const val requestResult = "setPinRequestResult"
    }
}
