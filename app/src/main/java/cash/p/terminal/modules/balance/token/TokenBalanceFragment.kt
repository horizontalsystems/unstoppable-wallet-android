package cash.p.terminal.modules.balance.token

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.entities.Wallet
import cash.p.terminal.modules.transactions.TransactionsModule
import cash.p.terminal.modules.transactions.TransactionsViewModel
import cash.p.terminal.ui.compose.ComposeAppTheme
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.parcelable

class TokenBalanceFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val wallet = requireArguments().parcelable<Wallet>(WALLET_KEY)
        if (wallet == null) {
            Toast.makeText(App.instance, "Wallet is Null", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
            return
        }
        val viewModel by viewModels<TokenBalanceViewModel> { TokenBalanceModule.Factory(wallet) }
        val transactionsViewModel by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

        ComposeAppTheme {
            TokenBalanceScreen(
                viewModel,
                transactionsViewModel,
                findNavController()
            )
        }
    }

    companion object {
        private const val WALLET_KEY = "wallet_key"

        fun prepareParams(wallet: Wallet) = bundleOf(
            WALLET_KEY to wallet
        )
    }
}
