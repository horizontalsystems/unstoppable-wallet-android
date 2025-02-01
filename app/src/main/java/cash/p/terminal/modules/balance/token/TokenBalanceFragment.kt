package cash.p.terminal.modules.balance.token

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.featureStacking.ui.staking.StackingType
import cash.p.terminal.modules.transactions.TransactionsModule
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.navigation.slideFromRight
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.isPirateCash
import io.horizontalsystems.core.getInput

class TokenBalanceFragment : BaseComposeFragment() {
    private var viewModel: TokenBalanceViewModel? = null

    @Composable
    override fun GetContent(navController: NavController) {
        val wallet = navController.getInput<Wallet>()
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            navController.popBackStack(R.id.tokenBalanceFragment, true)
            return
        }
        val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
        this.viewModel = viewModel
        val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

        TokenBalanceScreen(
            viewModel = viewModel,
            transactionsViewModel = transactionsViewModel,
            navController = navController,
            onStackingClicked = {
                navController.slideFromRight(
                    resId = R.id.stacking,
                    input = if(wallet.isPirateCash()) StackingType.PCASH else StackingType.COSANTA
                )
            }
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel?.startStatusChecker()
    }

    override fun onPause() {
        viewModel?.stopStatusChecker()
        super.onPause()
    }

}
