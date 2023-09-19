package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinUnlock
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.setNavigationResult

class PinFragment : BaseComposeFragment() {

    private val attachedToLockScreen: Boolean by lazy {
        arguments?.getBoolean(PinModule.keyAttachedToLockScreen) ?: false
    }

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
        if (attachedToLockScreen) {
            activity?.setResult(PinModule.RESULT_OK)
            activity?.finish()
            return
        }

        val bundle = bundleOf(
            PinModule.requestResult to PinModule.RESULT_OK
        )
        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

    private fun onCancelClick() {
        if (attachedToLockScreen) {
            activity?.setResult(PinModule.RESULT_CANCELLED)
            activity?.finish()
            return
        }

        val bundle = bundleOf(
            PinModule.requestResult to PinModule.RESULT_CANCELLED
        )
        setNavigationResult(PinModule.requestKey, bundle)
        findNavController().popBackStack()
    }

}
