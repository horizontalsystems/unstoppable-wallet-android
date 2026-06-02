package io.horizontalsystems.bankwallet.modules.transactions

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment

class TransactionsFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        val viewModel = viewModel<TransactionsViewModel>(factory = TransactionsModule.Factory())
        TransactionsScreen(navController, viewModel)
    }
}
