package com.quantum.wallet.bankwallet.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.BaseComposeFragment
import com.quantum.wallet.bankwallet.entities.Wallet
import com.quantum.wallet.bankwallet.modules.transactions.TransactionsModule
import com.quantum.wallet.bankwallet.modules.transactions.TransactionsViewModel

class TokenBalanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Wallet>(navController) { wallet ->
            val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
            val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

            TokenBalanceScreen(
                viewModel,
                transactionsViewModel,
                navController
            )
        }
    }
}
