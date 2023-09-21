package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinUnlock
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class UnlockPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinUnlock(
                showCancelButton = true,
                dismissWithSuccess = {
                    closeWithResult(PinModule.RESULT_OK)
                },
                onCancelClick = {
                    closeWithResult(PinModule.RESULT_CANCELLED)
                }
            )
        }
    }

    private fun closeWithResult(result: Int) {
        val bundle = bundleOf(PinModule.requestResult to result)
        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

}
