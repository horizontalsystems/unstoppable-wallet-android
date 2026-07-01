package io.horizontalsystems.bankwallet.modules.nav3

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.bankwallet.modules.main.MainActivityViewModel
import io.horizontalsystems.bankwallet.modules.main.MainScreenWithRootedDeviceCheck
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectNewPage
import io.horizontalsystems.bankwallet.modules.tonconnect.TonConnectSendRequestPage
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import kotlinx.coroutines.delay
import java.util.UUID

@Composable
fun MainScreen(navigation: HSNavigation, parentScreenContentKey: String) {
    val mainActivityViewModel =
        viewModel<MainActivityViewModel>(viewModelStoreOwner = LocalActivity.current as ComponentActivity)

    val activity = LocalActivity.current

    val tcSendRequest by mainActivityViewModel.tcSendRequest.observeAsState()
    LaunchedEffect(tcSendRequest) {
        if (tcSendRequest != null) {
            navigation.slideFromBottom(TonConnectSendRequestPage)
        }
    }

    val tcDappRequest by mainActivityViewModel.tcDappRequest.observeAsState()
    val uuid = rememberSaveable { UUID.randomUUID().toString() }
    ResultEffect<TonConnectNewPage.Result>(resultKeyUuid = uuid) { result ->
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
            val screen = TonConnectNewPage(tmpTcDappRequest.dAppRequest)
            screen.resultKey = uuid
            navigation.slideFromBottom(screen)
            mainActivityViewModel.onTcDappRequestHandled()
        }
    }

    MainScreenWithRootedDeviceCheck(
        transactionsViewModel = viewModel<TransactionsViewModel>(factory = TransactionsModule.Factory()),
        navigation = navigation,
        mainActivityViewModel = mainActivityViewModel,
        parentScreenContentKey = parentScreenContentKey
    )
}
