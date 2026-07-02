package io.horizontalsystems.core.modules.opencryptopay

import androidx.compose.runtime.Composable
import io.horizontalsystems.core.modules.nav3.HSNavigation
import io.horizontalsystems.core.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class OpenCryptoPayPage(val input: Input) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        OpenCryptoPayScreen(navigation = navigation, lnurl = input.lnurl)
    }

    @Serializable
    data class Input(val lnurl: String)
}
