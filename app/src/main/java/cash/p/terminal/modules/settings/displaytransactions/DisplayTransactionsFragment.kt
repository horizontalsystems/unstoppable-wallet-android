package cash.p.terminal.modules.settings.displaytransactions

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import cash.p.terminal.ui_compose.BaseComposeFragment
import cash.p.terminal.wallet.managers.TransactionDisplayLevel
import org.koin.android.ext.android.inject

class DisplayTransactionsFragment : BaseComposeFragment() {
    private val viewModel: DisplayTransactionsViewModel by inject()

    @Composable
    override fun GetContent(navController: NavController) {
        DisplayTransactionsScreen(
            selectedItem = viewModel.uiState.collectAsStateWithLifecycle(TransactionDisplayLevel.NOTHING).value,
            onItemSelected = {
                viewModel.onItemSelected(it)
                navController.popBackStack()
            },
            onBackPressed = {
                navController.popBackStack()
            }
        )
    }
}
