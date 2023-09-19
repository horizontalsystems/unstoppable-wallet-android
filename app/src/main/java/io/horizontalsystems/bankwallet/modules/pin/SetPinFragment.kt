package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
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
