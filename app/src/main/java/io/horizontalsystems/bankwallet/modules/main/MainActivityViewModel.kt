package io.horizontalsystems.bankwallet.modules.main

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2Request
import io.horizontalsystems.bankwallet.modules.walletconnect.version2.WC2SessionManager
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable

class MainActivityViewModel(
    wcSessionManager: WC2SessionManager,
): ViewModel() {

    private val disposables = CompositeDisposable()

    val openWalletConnectRequestLiveEvent = SingleLiveEvent<WC2Request>()

    init {
        wcSessionManager.pendingRequestObservable
            .subscribeIO{
                openWalletConnectRequestLiveEvent.postValue(it)
            }.let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }
}
