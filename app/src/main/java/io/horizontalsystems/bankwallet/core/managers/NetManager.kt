package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.netkit.NetKit
import io.horizontalsystems.tor.ConnectionStatus
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject

class NetManager(context: Context, val localStorage: ILocalStorage) : INetManager {

    override val torObservable = PublishSubject.create<Boolean>()

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
        disposables.add(kit.startTor(false)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({
                                               torObservable.onNext(it.connection.status == ConnectionStatus.CONNECTED)
                                               Log.i("NetManager",
                                                     "Tor connection: ${it.connection.status}, Tor state: ${it.status}")
                                           }, {
                                               Log.e("NetManager", "Tor exception", it)
                                           }))
    }

    override fun stop(): Single<Boolean> {
        return kit.stopTor()
    }


    override val isTorEnabled: Boolean
        get() = localStorage.torEnabled

}
