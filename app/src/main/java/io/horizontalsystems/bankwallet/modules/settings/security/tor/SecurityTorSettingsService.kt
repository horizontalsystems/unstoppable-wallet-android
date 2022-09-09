package io.horizontalsystems.bankwallet.modules.settings.security.tor

import android.util.Log
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.core.IPinComponent
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class SecurityTorSettingsService(
    private val torManager: ITorManager,
    private val pinComponent: IPinComponent,
) {

    private var disposables: CompositeDisposable = CompositeDisposable()

    private val torConnectionStatusSubject = BehaviorSubject.create<TorStatus>()
    val torConnectionStatusObservable: Observable<TorStatus>
        get() = torConnectionStatusSubject

    private val restartAppSubject = BehaviorSubject.create<Unit>()
    val restartAppObservable: Observable<Unit>
        get() = restartAppSubject

    var torEnabled: Boolean = torManager.isTorEnabled
        private set

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
        torManager.setTorAsEnabled()
        torManager.start()
        restartAppSubject.onNext(Unit)
    }

    fun disableTor() {
        torEnabled = false
        torManager.setTorAsDisabled()
        torManager.stop()
            .subscribe({
                pinComponent.updateLastExitDateBeforeRestart()
                restartAppSubject.onNext(Unit)
            }, {
                Log.e("SecurityTorSettingsService", "stopTor", it)
            })
            .let {
                disposables.add(it)
            }
    }
}
