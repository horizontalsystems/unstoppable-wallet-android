package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class WCSignMessageRequestViewModel(
        private val service: WCSignMessageRequestModule.RequestAction
) : ViewModel() {
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val message = service.message

    fun sign() {
        service.sign()
        closeLiveEvent.postValue(Unit)
    }

    fun reject() {
        service.reject()
        closeLiveEvent.postValue(Unit)
    }

}
