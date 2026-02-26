package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object WCListScreen : HSScreen()

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<Input>()
        WCSessionsScreen(
            navController,
            input?.deepLinkUri
        )
    }

    @Parcelize
    data class Input(val deepLinkUri: String) : Parcelable
}
