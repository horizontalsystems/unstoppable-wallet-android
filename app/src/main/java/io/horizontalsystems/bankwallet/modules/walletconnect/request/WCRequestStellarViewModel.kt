package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.walletconnect.web3.wallet.client.Wallet
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.walletconnect.WCDelegate
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WCRequestStellarPreViewModel : ViewModelUiState<DataState<WCRequestStellarPreUiState>>() {
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
                WCRequestStellarPreUiState(
                    wcAction = wcAction,
                    sessionRequest = sessionRequest,
                )
            )
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestStellarPreViewModel() as T
        }
    }
}

data class WCRequestStellarPreUiState(
    val wcAction: AbstractWCAction,
    val sessionRequest: Wallet.Model.SessionRequest
)

class WCRequestStellarViewModel(
    private val sessionRequest: Wallet.Model.SessionRequest,
    private val wcAction: AbstractWCAction
) : ViewModelUiState<WCRequestStellarUiState>() {

    private var finish: Boolean = false
    private var error: Throwable? = null

    private var actionState = wcAction.stateFlow.value

    init {
        viewModelScope.launch {
            wcAction.stateFlow.collect {
                actionState = it

                emitState()
            }
        }

        wcAction.start(viewModelScope)
    }

    override fun createState() = WCRequestStellarUiState(
        title = wcAction.getTitle(),
        finish = finish,
        runnable = actionState.runnable,
        approveButtonTitle = wcAction.getApproveButtonTitle(),
        contentItems = actionState.items
    )

    fun approve() = viewModelScope.launch(Dispatchers.Default) {
        error = null

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

        emitState()
    }

    fun reject() = viewModelScope.launch(Dispatchers.Default) {
        error = null

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

        emitState()
    }

    class Factory(
        private val sessionRequest: Wallet.Model.SessionRequest,
        private val wcAction: AbstractWCAction,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestStellarViewModel(sessionRequest, wcAction) as T
        }
    }
}

data class WCRequestStellarUiState(
    val title: TranslatableString,
    val finish: Boolean,
    val runnable: Boolean,
    val approveButtonTitle: TranslatableString,
    val contentItems: List<WCActionContentItem>,
)
