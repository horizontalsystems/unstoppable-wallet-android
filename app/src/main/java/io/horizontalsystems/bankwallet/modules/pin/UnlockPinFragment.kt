package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinUnlock
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class UnlockPinFragment : BaseComposeFragment() {

    private val showCancelButton: Boolean by lazy {
        arguments?.getBoolean(PinModule.keyShowCancel) ?: false
    }

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinUnlock(
                showCancelButton = showCancelButton,
                dismissWithSuccess = { dismissWithSuccess() },
                onCancelClick = { onCancelClick() }
            )
        }
    }

    private fun dismissWithSuccess() {
        val bundle = bundleOf(PinModule.requestResult to PinModule.RESULT_OK)
        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

    private fun onCancelClick() {
        val bundle = bundleOf(PinModule.requestResult to PinModule.RESULT_CANCELLED)
        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

}
