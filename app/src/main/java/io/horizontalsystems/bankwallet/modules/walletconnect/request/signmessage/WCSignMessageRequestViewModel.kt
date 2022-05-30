package io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.SingleLiveEvent

class WCSignMessageRequestViewModel(
    private val service: WCSignMessageRequestModule.RequestAction,
) : ViewModel() {
    val closeLiveEvent = SingleLiveEvent<Unit>()
    val message = service.message

    val dAppName by service::dAppName

    var trustCheckmarkChecked: Boolean by mutableStateOf(false)
        private set

    var signEnabled: Boolean by mutableStateOf(signButtonEnabled())
        private set

    fun sign() {
        service.sign()
        closeLiveEvent.postValue(Unit)
    }

    fun reject() {
        service.reject()
        closeLiveEvent.postValue(Unit)
    }

    fun onTrustChecked(checked: Boolean){
        trustCheckmarkChecked = checked
        signEnabled = signButtonEnabled()
    }

    private fun signButtonEnabled(): Boolean {
        return !service.isLegacySignRequest || trustCheckmarkChecked
    }

}
