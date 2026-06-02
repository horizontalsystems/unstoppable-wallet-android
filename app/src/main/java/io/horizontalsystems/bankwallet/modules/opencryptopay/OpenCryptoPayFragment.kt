package io.horizontalsystems.bankwallet.modules.opencryptopay

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class OpenCryptoPayFragment(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        OpenCryptoPayScreen(navController = navController, lnurl = input.lnurl)
    }

    @Serializable
    data class Input(val lnurl: String)
}
