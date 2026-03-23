package io.horizontalsystems.bankwallet.modules.tonconnect

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.parcelize.Parcelize

class TonConnectMainFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val input = navController.getInput<Input>()
        TonConnectMainScreen(navController, input?.deepLinkUri)
    }

    @Parcelize
    data class Input(val deepLinkUri: String) : Parcelable
}
