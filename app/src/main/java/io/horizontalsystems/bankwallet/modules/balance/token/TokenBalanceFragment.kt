package io.horizontalsystems.bankwallet.modules.balance.token

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class TokenBalanceScreen(val wallet: Wallet) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        val viewModel = viewModel<TokenBalanceViewModel>(factory = TokenBalanceModule.Factory(wallet))

        TokenBalanceScreen(
            viewModel,
            backStack
        )
    }
}

class TokenBalanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        withInput<Wallet>(navController) { wallet ->
//            val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
//
//            TokenBalanceScreen(
//                viewModel,
//                navController
//            )
        }
    }
}
