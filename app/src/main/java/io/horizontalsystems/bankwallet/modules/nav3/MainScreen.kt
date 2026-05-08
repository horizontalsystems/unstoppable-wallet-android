package io.horizontalsystems.bankwallet.modules.nav3

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainScreenWithRootedDeviceCheck
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectNewFragment
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectSendRequestFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data object MainScreen : HSScreen() {
    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        val mainActivityViewModel =
            viewModel<MainActivityViewModel>(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

        val activity = LocalActivity.current

        val tcSendRequest by mainActivityViewModel.tcSendRequest.observeAsState()
        LaunchedEffect(tcSendRequest) {
            if (tcSendRequest != null) {
                navController.slideFromBottom(TonConnectSendRequestFragment)
            }
        }

        val tcDappRequest by mainActivityViewModel.tcDappRequest.observeAsState()
        val uuid = rememberSaveable { UUID.randomUUID().toString() }
        ResultEffect<TonConnectNewFragment.Result>(resultKeyUuid = uuid) { result ->
            if (tcDappRequest?.closeAppOnResult == true) {
                if (result.approved) {
                    //Need delay to get connected before closing activity
                    delay(1000)
                }
                activity?.finish()
            }
        }

        LaunchedEffect(tcDappRequest) {
            val tmpTcDappRequest = tcDappRequest
            if (tmpTcDappRequest != null) {
                val screen = TonConnectNewFragment(tmpTcDappRequest.dAppRequest)
                screen.resultKey = uuid
                navController.slideFromBottom(screen)
                mainActivityViewModel.onTcDappRequestHandled()
            }
        }

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
