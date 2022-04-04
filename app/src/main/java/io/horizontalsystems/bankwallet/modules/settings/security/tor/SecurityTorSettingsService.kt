package io.horizontalsystems.bankwallet.modules.settings.security.tor

import android.util.Log
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.core.IWalletManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class SecurityTorSettingsService(
    private val torManager: ITorManager,
    private val pinComponent: IPinComponent,
    private val walletManager: IWalletManager,
) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    val needToRestartAppForTor: Boolean
        get() = walletManager.activeWallets.isNotEmpty()

    private val torConnectionStatusSubject = BehaviorSubject.create<TorStatus>()
    val torConnectionStatusObservable: Observable<TorStatus>
        get() = torConnectionStatusSubject

    private val restartAppSubject = BehaviorSubject.create<Unit>()
    val restartAppObservable: Observable<Unit>
        get() = restartAppSubject

    var torEnabled: Boolean
        get() = torManager.isTorEnabled
        private set(value) {
            pinComponent.updateLastExitDateBeforeRestart()
            if (value) {
                torManager.setTorAsEnabled()
            } else {
                torManager.setTorAsDisabled()
            }
        }

    val isTorNotificationEnabled: Boolean
        get() = torManager.isTorNotificationEnabled

    fun start() {
        torConnectionStatusSubject.onNext(TorStatus.Closed)
        torManager.torObservable.subscribe { connectionStatus ->
            if (connectionStatus == TorStatus.Failed) {
                torEnabled = false
            } else if (connectionStatus == TorStatus.Connected) {
                torEnabled = true
            }
            torConnectionStatusSubject.onNext(connectionStatus)
        }.let {
            disposables.add(it)
        }
    }

    fun stop() {
        disposables.clear()
    }

    fun enableTor() {
        torEnabled = true
        torManager.start()
    }

    fun disableTor() {
        torEnabled = false
        torManager.stop()
            .subscribe({
                if (needToRestartAppForTor) {
                    restartAppSubject.onNext(Unit)
                }
            }, {
                Log.e("SecurityTorSettingsService", "stopTor", it)
            })
            .let {
                disposables.add(it)
            }
    }
}
