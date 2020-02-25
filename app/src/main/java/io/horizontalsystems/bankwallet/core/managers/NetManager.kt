package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.netkit.NetKit
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class NetManager(context: Context, localStorage: ILocalStorage) : INetManager {

    private val disposables = CompositeDisposable()
    private val kit: NetKit by lazy {
        NetKit(context)
    }

    init {
        if (localStorage.torEnabled){
            start()
        }
    }

    override fun start() {
        disposables.add(kit.startTor(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.i("NetManager", "Tor connection: ${it.connection.getState()}, Tor state: ${it.state}")
                }, {
                    Log.e("NetManager", "Tor exception", it)
                }))
    }

    override fun stop(): Single<Boolean> {
        return kit.stopTor()
    }

}
