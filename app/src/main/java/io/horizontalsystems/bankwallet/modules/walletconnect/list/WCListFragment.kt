package io.horizontalsystems.bankwallet.modules.walletconnect.list

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.list.ui.WCSessionsScreen
import kotlinx.serialization.Serializable

@Serializable
data class WCListFragment(val input: Input? = null) : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        WCSessionsScreen(
            navController,
            input?.deepLinkUri
        )
    }

    @Serializable
    data class Input(val deepLinkUri: String)
}
