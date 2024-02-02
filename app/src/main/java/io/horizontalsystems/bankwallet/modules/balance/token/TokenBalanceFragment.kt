package io.horizontalsystems.bankwallet.modules.balance.token

import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel

class TokenBalanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val wallet = navController.getInput<Wallet>()
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.tokenBalanceFragment, true)
            return
        }
        val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
        val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

        BlockDrawingUntil(viewModel.uiState.transactions != null)

        TokenBalanceScreen(
            viewModel,
            transactionsViewModel,
            navController
        )
    }

}

@Composable
fun BlockDrawingUntil(isReady: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, isReady) {
        val onPreDrawListener = ViewTreeObserver.OnPreDrawListener { isReady }
        view.viewTreeObserver.addOnPreDrawListener(onPreDrawListener)
        onDispose {
            view.viewTreeObserver.removeOnPreDrawListener(onPreDrawListener)
        }
    }
}
