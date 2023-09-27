package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import kotlinx.parcelize.Parcelize

class SetPinFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val navController = findNavController()
        val input = navController.getInput<Input>()

        ComposeAppTheme {
            PinSet(
                title = stringResource(R.string.PinSet_Title),
                description = stringResource(input?.descriptionResId ?: R.string.PinSet_Info),
                dismissWithSuccess = {
                    navController.setNavigationResultX(Result(true))
                    navController.popBackStack()
                },
                onBackPress = { navController.popBackStack() }
            )
        }
    }

    @Parcelize
    data class Input(val descriptionResId: Int) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
