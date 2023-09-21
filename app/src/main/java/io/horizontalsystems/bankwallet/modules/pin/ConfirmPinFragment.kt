package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinConfirm
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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
