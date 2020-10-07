package io.horizontalsystems.bankwallet.modules.walletconnect.scanqr

import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.core.SingleLiveEvent

class WalletConnectScanQrPresenter(private val service: WalletConnectService) {

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
