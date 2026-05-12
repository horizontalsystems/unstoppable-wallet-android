package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.MainScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import kotlinx.serialization.Serializable

@Serializable
data class TokenBalanceFragment(val wallet: Wallet) : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        val viewModel = viewModel<TokenBalanceViewModel>(factory = TokenBalanceModule.Factory(wallet))
        val transactionsViewModel = navController.viewModelForScreen<TransactionsViewModel>(MainScreen::class)

        TokenBalanceScreen(
            viewModel,
            transactionsViewModel,
            navController
        )
    }
}
