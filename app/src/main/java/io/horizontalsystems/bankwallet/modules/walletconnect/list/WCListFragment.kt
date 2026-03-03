package io.horizontalsystems.bankwallet.modules.walletconnect.list

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data class WCListScreen(val deepLinkUri: String? = null) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        WCSessionsScreen(
            backStack,
            deepLinkUri
        )
    }
}

class WCListFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
//        val input = navController.getInput<Input>()
//        WCSessionsScreen(
//            navController,
//            input?.deepLinkUri
//        )
    }

    @Parcelize
    data class Input(val deepLinkUri: String) : Parcelable
}
