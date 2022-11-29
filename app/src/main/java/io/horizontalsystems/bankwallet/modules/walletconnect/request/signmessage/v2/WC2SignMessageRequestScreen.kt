package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestModule
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WCSignMessageRequestViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.ui.SignMessageRequestScreen
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

@Composable
fun WC2SignMessageRequestScreen(
    navController: NavController,
    requestData: WC2SessionManager.RequestData
) {
    val viewModel = viewModel<WCSignMessageRequestViewModel>(
        factory = WCSignMessageRequestModule.FactoryWC2(
            requestData
        )
    )

    val close by viewModel.closeLiveEvent.observeAsState()
    LaunchedEffect(close) {
        if (close != null) {
            navController.popBackStack()
        }
    }

    SignMessageRequestScreen(
        navController,
        viewModel,
    )
}
