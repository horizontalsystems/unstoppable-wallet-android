package io.horizontalsystems.core.modules.walletconnect.list

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import io.horizontalsystems.core.modules.walletconnect.list.ui.WCSessionsScreen
import kotlinx.serialization.Serializable

@Serializable
data class WCListPage(val input: Input? = null) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        WCSessionsScreen(
            navigation,
            input?.deepLinkUri
        )
    }

    @Serializable
    data class Input(val deepLinkUri: String)
}
