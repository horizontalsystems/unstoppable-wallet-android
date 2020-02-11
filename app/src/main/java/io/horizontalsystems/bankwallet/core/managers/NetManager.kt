package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.INetManager
import io.horizontalsystems.netkit.NetKit
import io.horizontalsystems.tor.Tor
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class NetManager(context: Context, localStorage: ILocalStorage) : INetManager, Tor.Listener {

    private val disposables = CompositeDisposable()
    private val kit: NetKit by lazy {
        NetKit(context, this)
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

                }, {

                }))
    }

    override fun stop(): Single<Boolean> {
        return kit.stopTor()
    }

    //Tor.Listener

    override fun onConnStatusUpdate(torConnInfo: Tor.ConnectionInfo?, message: String) {
        Log.e("NetKitManager", "onConnStatusUpdate: ${torConnInfo?.connectionState}, $message")
    }

    override fun onProcessStatusUpdate(torInfo: Tor.Info?, message: String) {
        Log.e("NetKitManager", "onProcessStatusUpdate: ${torInfo?.state}, $message")
    }
}