package io.horizontalsystems.bankwallet.modules.tor

import io.horizontalsystems.bankwallet.core.INetManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class TorStatusInteractor(private val netManager: INetManager) : TorStatusModule.Interactor {

    var delegate: TorStatusModule.InteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun subscribeToEvents() {
        disposables.add(netManager.torObservable
                .subscribe { connectionStatus ->
                    delegate?.updateConnectionStatus(connectionStatus)
                })
    }

    override fun restartTor() {
        netManager.enableTor()
        netManager.start()
    }

    override fun disableTor() {
        netManager.disableTor()
        disposables.add(netManager.stop()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    delegate?.didStopTor()
                }, {

                }))
    }

    override fun clear() {
        disposables.dispose()
    }
}
