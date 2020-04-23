package io.horizontalsystems.bankwallet.modules.tor

import io.horizontalsystems.bankwallet.core.ITorManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class TorStatusInteractor(private val torManager: ITorManager) : TorStatusModule.Interactor {

    var delegate: TorStatusModule.InteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override fun subscribeToEvents() {
        disposables.add(torManager.torObservable
                .subscribe { connectionStatus ->
                    delegate?.updateConnectionStatus(connectionStatus)
                })
    }

    override fun restartTor() {
        torManager.enableTor()
        torManager.start()
    }

    override fun disableTor() {
        torManager.disableTor()
        disposables.add(torManager.stop()
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
