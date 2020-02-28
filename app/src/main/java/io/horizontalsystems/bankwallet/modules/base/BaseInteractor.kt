package io.horizontalsystems.bankwallet.modules.base

import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.reactivex.disposables.Disposable

class BaseInteractor(private val netManager: INetManager): BaseModule.Interactor {

    var delegate: BaseModule.InteractorDelegate? = null
    private var disposable: Disposable? = null

    override fun subscribeToEvents() {
        if (netManager.isTorEnabled) {
            disposable = netManager.torObservable
                    .subscribe { connectionStatus ->
                        if(connectionStatus != TorStatus.Connected)
                            delegate?.showTorConnectionStatus()
                    }
        }
    }

    override fun clear() {
        disposable?.dispose()
    }
}
