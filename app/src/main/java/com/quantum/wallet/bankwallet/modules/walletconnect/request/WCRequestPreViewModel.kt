package com.quantum.wallet.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.reown.walletkit.client.Wallet
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.bankwallet.entities.DataState
import com.quantum.wallet.bankwallet.modules.walletconnect.WCDelegate

class WCRequestPreViewModel : ViewModelUiState<DataState<WCRequestPreUiState>>() {
    private val sessionRequest = WCDelegate.sessionRequestEvent
    private val wcAction = App.wcManager.getActionForRequest(sessionRequest)

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

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestPreViewModel() as T
        }
    }
}

data class WCRequestPreUiState(
    val wcAction: AbstractWCAction,
    val sessionRequest: Wallet.Model.SessionRequest
)