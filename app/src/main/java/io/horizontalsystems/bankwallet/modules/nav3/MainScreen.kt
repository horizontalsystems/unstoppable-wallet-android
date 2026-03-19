package io.horizontalsystems.bankwallet.modules.nav3

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainScreenWithRootedDeviceCheck
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import kotlinx.serialization.Serializable

@Serializable
data object MainScreen : HSScreen() {
    // TODO("Nav3: need to find other solution. There should not be mainActivityViewModel")
    lateinit var mainActivityViewModel: MainActivityViewModel

    @Composable
    override fun GetContent(navController: NavController) {
        MainScreenWithRootedDeviceCheck(
            transactionsViewModel = viewModel<TransactionsViewModel>(factory = TransactionsModule.Factory()),
            navController = navController,
            mainActivityViewModel = mainActivityViewModel,
        )
    }

    //    @Composable
//    override fun GetContent(backStack: NavBackStack<HSScreen>) {
//        MainScreenWithRootedDeviceCheck(
//            mainActivityViewModel = mainActivityViewModel,
//            backStack = backStack,
//        )
//    }
}
