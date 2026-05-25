package io.horizontalsystems.bankwallet.modules.walletconnect.request

import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.modules.walletconnect.WCManager
import io.horizontalsystems.dapp.core.HSDAppRequest
import javax.inject.Inject

@HiltViewModel
class WCRequestPreViewModel @Inject constructor(
    private val wcManager: WCManager
) : ViewModelUiState<DataState<WCRequestPreUiState>>() {
    private val sessionRequest = WCDelegate.sessionRequestEvent
    private val wcAction = wcManager.getActionForRequest(sessionRequest)

    override fun createState() = when {
        sessionRequest == null -> {
            DataState.Error(Exception("No request"))
        }

        wcAction == null -> {
            DataState.Error(Exception("No action for request"))
        }

        else -> {
            DataState.Success(
                WCRequestPreUiState(
                    wcAction = wcAction,
                    sessionRequest = sessionRequest,
                )
            )
        }
    }
}

data class WCRequestPreUiState(
    val wcAction: AbstractWCAction,
    val sessionRequest: HSDAppRequest
)