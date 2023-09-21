package io.horizontalsystems.bankwallet.modules.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController

class SetPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        ComposeAppTheme {
            PinSet(
                title = stringResource(R.string.PinSet_Title),
                description = stringResource(R.string.PinSet_Info),
                dismissWithSuccess = { findNavController().popBackStack() },
                onBackPress = { findNavController().popBackStack() }
            )
        }
    }
}
