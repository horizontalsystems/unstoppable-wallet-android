package io.horizontalsystems.bankwallet.base

import io.horizontalsystems.bankwallet.core.INetManager
import io.reactivex.disposables.Disposable

class BaseInteractor(private val netManager: INetManager): BaseModule.Interactor {

    var delegate: BaseModule.InteractorDelegate? = null
    private var disposable: Disposable? = null

    override fun subscribeToEvents() {
        if (netManager.isTorEnabled) {
            disposable = netManager.torObservable
                    .subscribe { isConnected ->
                        if(!isConnected)
                            delegate?.showTorConnectionStatus()
                    }
        }
    }

    override fun clear() {
        disposable?.dispose()
    }
}
