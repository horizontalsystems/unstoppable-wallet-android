package io.horizontalsystems.bankwallet.modules.torpage

import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.bankwallet.core.managers.TorStatus
import io.reactivex.disposables.CompositeDisposable

class TorPageInteractor(private var netManager: INetManager) : TorPageModule.IInteractor {

    var delegate: TorPageModule.InteractorDelegate? = null
    private var disposables: CompositeDisposable = CompositeDisposable()

    override val isTorNotificationEnabled: Boolean
        get() = netManager.isTorNotificationEnabled

    override var isTorEnabled: Boolean
        get() = netManager.isTorEnabled
        set(value) {
            if (value) {
                netManager.enableTor()
            } else {
                netManager.disableTor()
            }
        }

    override fun onViewLoad() {
        delegate?.updateConnectionStatus(TorStatus.Closed)
        disposables.add(netManager.torObservable
                .subscribe { connectionStatus ->
                    delegate?.updateConnectionStatus(connectionStatus)
                })
    }

    override fun enableTor() {
        netManager.start()
    }

    override fun disableTor() {
        disposables.add(netManager.stop()
                .subscribe())
    }

    override fun onClear() {
        disposables.dispose()
    }
}
