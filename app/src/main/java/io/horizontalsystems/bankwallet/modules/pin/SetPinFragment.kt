package io.horizontalsystems.bankwallet.modules.pin

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.pin.ui.PinSet
import kotlinx.parcelize.Parcelize

class SetPinFragment(val input: Input? = null) : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        PinSet(
            title = stringResource(R.string.PinSet_Title),
            description = stringResource(input?.descriptionResId ?: R.string.PinSet_Info),
            dismissWithSuccess = {
                navController.setNavigationResultX(Result(true))
                navController.removeLastOrNull()
            },
            onBackPress = { navController.removeLastOrNull() }
        )
    }

    @Parcelize
    data class Input(val descriptionResId: Int) : Parcelable

    @Parcelize
    data class Result(val success: Boolean) : Parcelable
}
