package cash.p.terminal.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.modules.pin.ui.PinConfirm
import cash.p.terminal.ui.compose.ComposeAppTheme
>>>>>>>> 5484c4753 (Divide PinUnlock to PinConfirm and PinUnlock):app/src/main/java/cash/p/terminal/modules/pin/ConfirmPinFragment.kt
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class ConfirmPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinConfirm(
                onSuccess = {
                    closeWithResult(PinModule.RESULT_OK)
                },
                onCancel = {
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
