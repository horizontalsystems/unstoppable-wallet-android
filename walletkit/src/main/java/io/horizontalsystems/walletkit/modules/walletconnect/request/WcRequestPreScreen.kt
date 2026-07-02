package io.horizontalsystems.walletkit.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation

@Composable
fun WcRequestPreScreen(navigation: HSNavigation) {
    val viewModelPre = viewModel<WCRequestPreViewModel>(
        factory = WCRequestPreViewModel.Factory()
    )

    val uiState = viewModelPre.uiState

    if (uiState is DataState.Success) {
        WcRequestScreen(navigation, uiState.data.sessionRequest, uiState.data.wcAction)
    } else if (uiState is DataState.Error) {
        WcRequestError { navigation.removeLastOrNull() }
    }
}
