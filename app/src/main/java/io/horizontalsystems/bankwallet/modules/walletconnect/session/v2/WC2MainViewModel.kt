package io.horizontalsystems.bankwallet.modules.walletconnect.session.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.SingleLiveEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class WC2MainViewModel(
    private val wc2Service: WC2Service,
    private val wcSessionManager: WC2SessionManager,
) : ViewModel() {

    val sessionProposalLiveEvent = SingleLiveEvent<Unit>()
    val openWalletConnectRequestLiveEvent = SingleLiveEvent<Long>()

    init {
        viewModelScope.launch {
            wc2Service.eventObservable.asFlow()
                .collect {
                    if (it is WC2Service.Event.WaitingForApproveSession) {
                        sessionProposalLiveEvent.postValue(Unit)
                    }
                }
        }

        viewModelScope.launch {
            wcSessionManager.pendingRequestObservable.asFlow()
                .collect {
                    openWalletConnectRequestLiveEvent.postValue(it)
                }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WC2MainViewModel(App.wc2Service, App.wc2SessionManager) as T
        }
    }
}
