package io.horizontalsystems.walletkit.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.WCDelegate
import io.horizontalsystems.walletkit.ui.compose.TranslatableString
import io.horizontalsystems.dapp.core.HSDAppRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WCRequestViewModel(
    private val sessionRequest: HSDAppRequest,
    private val wcAction: AbstractWCAction,
    private val accountManager: IAccountManager
) : ViewModelUiState<WCRequestUiState>() {

    private var approveInProgress = false
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

    override fun createState() = WCRequestUiState(
        walletName = accountManager.activeAccount?.name,
        title = wcAction.getTitle(),
        finish = finish,
        runnable = !approveInProgress && actionState.runnable,
        approveButtonTitle = wcAction.getApproveButtonTitle(),
        contentItems = actionState.items,
        error = error
    )

    fun approve() = viewModelScope.launch(Dispatchers.Default) {
        error = null
        approveInProgress = true
        emitState()

        try {
            val actionResult = wcAction.performAction()

            WCDelegate.respondPendingRequest(
                sessionRequest.requestId,
                sessionRequest.topic,
                actionResult,
                onSuccessResult = {
                    approveInProgress = false
                    finish = true
                    emitState()
                },
                onErrorResult = {
                    approveInProgress = false
                    error = it
                    emitState()
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            approveInProgress = false
            error = e
            emitState()
        }
    }

    fun reject() = viewModelScope.launch(Dispatchers.Default) {
        error = null

        WCDelegate.rejectRequest(
            sessionRequest.topic,
            sessionRequest.requestId,
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
        private val sessionRequest: HSDAppRequest,
        private val wcAction: AbstractWCAction,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WCRequestViewModel(sessionRequest, wcAction, App.accountManager) as T
        }
    }
}

data class WCRequestUiState(
    val walletName: String?,
    val title: TranslatableString,
    val finish: Boolean,
    val runnable: Boolean,
    val approveButtonTitle: TranslatableString,
    val contentItems: List<SectionViewItem>,
    val error: Throwable?
)
