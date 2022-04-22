package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class WCSignMessageRequestViewModel(
    private val service: WCSignMessageRequestModule.RequestAction,
) : ViewModel() {
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val message = service.message
    val signEnabled by service::signButtonEnabled
    val trustCheckmarkChecked by service::trustCheckmarkChecked

    fun sign() {
        service.sign()
        closeLiveEvent.postValue(Unit)
    }

    fun reject() {
        service.reject()
        closeLiveEvent.postValue(Unit)
    }

    fun onTrustChecked(checked: Boolean){
        service.onTrustChecked(checked)
    }

}
