package cash.p.terminal.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import cash.p.terminal.R
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.setNavigationResultX
import cash.p.terminal.modules.pin.ui.PinSet
import cash.p.terminal.ui.compose.ComposeAppTheme
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
