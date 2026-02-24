package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet

class TokenBalanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Wallet>(navController) { wallet ->
            val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }

            TokenBalanceScreen(
                viewModel,
                navController
            )
        }
    }
}
