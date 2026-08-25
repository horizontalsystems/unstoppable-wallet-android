package io.horizontalsystems.walletkit.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class TokenBalancePage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<TokenBalanceViewModel>(factory = TokenBalanceModule.Factory(wallet))

        TokenBalanceScreen(
            viewModel,
            navigation
        )
    }
}
