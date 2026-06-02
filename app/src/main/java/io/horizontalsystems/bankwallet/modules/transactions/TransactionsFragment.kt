package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class TransactionsFragment : BaseComposeFragment() {
    private val viewModel: TransactionsViewModel by navGraphViewModels(R.id.mainFragment) {
        TransactionsModule.Factory()
    }

    @Composable
    override fun GetContent(navController: NavController) {
        TransactionsScreen(navController, viewModel)
    }
}
