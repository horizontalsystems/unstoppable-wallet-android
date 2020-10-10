package io.horizontalsystems.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent

class WalletConnectRequestViewModel(private val service: WalletConnectService, private val requestId: Long) : ViewModel() {

    val closeLiveEvent = SingleLiveEvent<Unit>()

    init {
        val request = service.getRequest(requestId)

        when(request?.type) {
            is WalletConnectService.Request.Type.SendEthereumTransaction -> {

            }
            is WalletConnectService.Request.Type.SignEthereumTransaction -> {

            }
        }
    }

    fun approve() {
        service.approveRequest(requestId)
        closeLiveEvent.postValue(Unit)
    }

    fun reject() {
        service.rejectRequest(requestId)
        closeLiveEvent.postValue(Unit)
    }

}
