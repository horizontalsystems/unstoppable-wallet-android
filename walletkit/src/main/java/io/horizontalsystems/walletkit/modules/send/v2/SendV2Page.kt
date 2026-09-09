package io.horizontalsystems.walletkit.modules.send.v2

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

/** Send screen for every blockchain type. */
@Serializable
data class SendV2Page(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<SendViewModel>(factory = SendViewModel.Factory(wallet))
        SendV2Screen(navigation, viewModel)
    }
}
