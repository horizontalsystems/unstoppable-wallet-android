package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WCRequestStellarViewModel : ViewModelUiState<WCRequestStellarUiState>() {
    private val sessionRequest = WCDelegate.sessionRequestEvent
    private val wcAction = App.wcManager.getActionForRequest(sessionRequest)

    private var finish: Boolean = false
    private var error: Throwable? = null

    override fun createState() = WCRequestStellarUiState(
        title = wcAction?.getTitle(),
        finish = finish
    )

    fun allow() = viewModelScope.launch(Dispatchers.Default) {
        error = null

        if (sessionRequest == null) {
            error = Exception("No request")
        } else if (wcAction == null) {
            error = Exception("No action for request")
        } else {
            val actionResult = wcAction.performAction()

            WCDelegate.respondPendingRequest(
                sessionRequest.request.id,
                sessionRequest.topic,
                actionResult,
                onSuccessResult = {
                    finish = true
                    emitState()
                },
                onErrorResult = {
                    error = it
                    emitState()
                }
            )
        }

        emitState()
    }

    fun reject() = viewModelScope.launch(Dispatchers.Default) {
        error = null

        if (sessionRequest != null) {
            WCDelegate.rejectRequest(
                sessionRequest.topic,
                sessionRequest.request.id,
                onSuccessResult = {
                    finish = true
                    emitState()
                },
                onErrorResult = {
                    error = it
                    emitState()
                }
            )
        } else {
            error = Exception("Empty session request")
        }

        emitState()
    }

    @Composable
    fun ScreenContent() {
        wcAction?.ScreenContent()
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestStellarViewModel() as T
        }
    }
}

data class WCRequestStellarUiState(
    val title: TranslatableString?,
    val finish: Boolean,
)
