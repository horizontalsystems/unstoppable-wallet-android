package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.compose.components.ListErrorView

@Composable
fun WcRequestPreScreen(navController: NavController) {
    val viewModelPre = viewModel<WCRequestPreViewModel>(
        factory = WCRequestPreViewModel.Factory()
    )

    val uiState = viewModelPre.uiState

    if (uiState is DataState.Success) {
        WcRequestScreen(navController, uiState.data.sessionRequest, uiState.data.wcAction)
    } else if (uiState is DataState.Error) {
        ListErrorView(uiState.error.message ?: "Error") { }
    }
}
