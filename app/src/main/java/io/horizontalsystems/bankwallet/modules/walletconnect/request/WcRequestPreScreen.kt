package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation

@Composable
fun WcRequestPreScreen(navController: HSNavigation) {
    val viewModelPre = hiltViewModel<WCRequestPreViewModel>()

    val uiState = viewModelPre.uiState

    if (uiState is DataState.Success) {
        WcRequestScreen(navController, uiState.data.sessionRequest, uiState.data.wcAction)
    } else if (uiState is DataState.Error) {
        WcRequestError { navController.removeLastOrNull() }
    }
}
