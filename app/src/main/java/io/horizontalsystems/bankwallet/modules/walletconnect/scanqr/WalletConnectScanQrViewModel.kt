package io.horizontalsystems.bankwallet.modules.walletconnect.scanqr

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent

class WalletConnectScanQrViewModel(private val service: WalletConnectService) : ViewModel() {

    val openMainLiveEvent = SingleLiveEvent<Unit>()
    val openErrorLiveEvent = SingleLiveEvent<Throwable>()

    fun handleScanned(string: String) {
        try {
            service.connect(string)
            openMainLiveEvent.postValue(Unit)
        } catch (t: Throwable) {
            openErrorLiveEvent.postValue(t)
        }
    }

}
