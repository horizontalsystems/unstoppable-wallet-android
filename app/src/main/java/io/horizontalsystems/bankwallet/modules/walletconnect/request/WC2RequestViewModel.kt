package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager

class WC2RequestViewModel(private val sessionManager: WC2SessionManager, private val requestId: Long) : ViewModel() {
    val requestData: WC2SessionManager.RequestData?

    init {
        requestData = try {
            sessionManager.createRequestData(requestId)
        } catch (e: WC2SessionManager.RequestDataError) {
            null
        }
    }

    class Factory(private val requestId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WC2RequestViewModel(App.wc2SessionManager, requestId) as T
        }
    }
}
