package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class SetPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
        ComposeAppTheme {
            PinSet(
                title = stringResource(R.string.PinSet_Title),
                description = stringResource(R.string.PinSet_Info),
                dismissWithSuccess = {
                    navController.setNavigationResultX(Result(true))
                    navController.popBackStack()
                },
                onBackPress = { navController.popBackStack() }
            )
        }
    }

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
