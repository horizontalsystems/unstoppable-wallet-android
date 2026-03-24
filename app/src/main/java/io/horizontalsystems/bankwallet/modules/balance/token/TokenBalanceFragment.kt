package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.MainScreen
import io.horizontalsystems.bankwallet.modules.nav3.viewModelForScreen
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel

class TokenBalanceFragment(val wallet: Wallet) : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val viewModel = viewModel<TokenBalanceViewModel>(factory = TokenBalanceModule.Factory(wallet))
        val transactionsViewModel = navController.viewModelForScreen<TransactionsViewModel>(MainScreen::class)

        TokenBalanceScreen(
            viewModel,
            transactionsViewModel,
            navController
        )
    }
}
