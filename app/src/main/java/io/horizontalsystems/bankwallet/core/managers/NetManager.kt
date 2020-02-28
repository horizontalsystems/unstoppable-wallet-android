package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.netkit.NetKit
import io.horizontalsystems.tor.ConnectionStatus
import io.horizontalsystems.tor.Tor
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class NetManager(context: Context, val localStorage: ILocalStorage) : INetManager {

    override val torObservable = PublishSubject.create<TorStatus>()

    private val disposables = CompositeDisposable()
    private val kit: NetKit by lazy {
        NetKit(context)
    }

    init {
        if (localStorage.torEnabled) {
            start()
        }
    }

    override fun start() {
        disposables.add(kit.torInfoSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    torObservable.onNext(getStatus(it))
                }, {
                    Log.e("NetManager", "Tor exception", it)
                })
        )
        kit.startTor(false)
    }

    override fun stop(): Single<Boolean> {
        return kit.stopTor()
    }

    override fun enableTor() {
        localStorage.torEnabled = true
    }

    override fun disableTor() {
        localStorage.torEnabled = false
    }

    override val isTorEnabled: Boolean
        get() = localStorage.torEnabled

    fun getStatus(torinfo: Tor.Info): TorStatus {
        return when (torinfo.connection.status) {
            ConnectionStatus.CONNECTED ->TorStatus.Connected
            ConnectionStatus.CONNECTING ->TorStatus.Connecting
            ConnectionStatus.FAILED ->TorStatus.Failed
            ConnectionStatus.CLOSED ->TorStatus.Failed
        }
    }

}

enum class TorStatus(val value: String) {
    Connected("Connected"),
    Connecting("Connecting"),
    Failed("Failed");
}
