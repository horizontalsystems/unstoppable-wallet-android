package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

/** Fee settings of the chain's send-transaction service owned by [SendV2ConfirmPage]. */
@Serializable
data class SendV2SettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<SendV2ConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetSettingsContent(navigation)
    }
}

/** Nonce settings of the chain's send-transaction service owned by [SendV2ConfirmPage]. */
@Serializable
data class SendV2NonceSettingsPage(val parentScreenContentKey: String) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = navigation.viewModelForScreen<SendV2ConfirmViewModel>(parentScreenContentKey)
        viewModel.sendTransactionService.GetNonceSettingsContent(navigation)
    }
}
