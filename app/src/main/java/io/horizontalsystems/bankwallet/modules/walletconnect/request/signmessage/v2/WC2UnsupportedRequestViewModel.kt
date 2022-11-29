package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.v2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Service
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

class WC2UnsupportedRequestViewModel(
    private val wc2Service: WC2Service,
    private val requestData: WC2SessionManager.RequestData
) : ViewModel() {

    fun reject() {
        wc2Service.rejectRequest(requestData.pendingRequest.topic, requestData.pendingRequest.id)
    }

    class Factory(private val requestData: WC2SessionManager.RequestData) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WC2UnsupportedRequestViewModel(App.wc2Service, requestData) as T
        }
    }
}
