package io.horizontalsystems.walletkit.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.Wallet
import io.horizontalsystems.walletkit.modules.nav3.EntryPage
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.transactions.TransactionsViewModel
import kotlinx.serialization.Serializable

@Serializable
data class TokenBalancePage(val wallet: Wallet) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val viewModel = viewModel<TokenBalanceViewModel>(factory = TokenBalanceModule.Factory(wallet))
        val transactionsViewModel = navigation.viewModelForScreen<TransactionsViewModel>(EntryPage::class)

        TokenBalanceScreen(
            viewModel,
            transactionsViewModel,
            navigation
        )
    }
}
