package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.netkit.NetKit
import io.horizontalsystems.tor.Tor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable

class NetManager : Tor.Listener {

    private val disposables = CompositeDisposable()
    private val kit: NetKit by lazy {
        NetKit(App.instance, this)
    }

    fun start() {
        disposables.add(kit.startTor(false)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {

                }))
    }

    fun stop() {
        disposables.add(kit.stopTor()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

                }, {

                }))
    }

    override fun onConnStatusUpdate(torConnInfo: Tor.ConnectionInfo?, message: String) {
    }

    override fun onProcessStatusUpdate(torInfo: Tor.Info?, message: String) {
    }
}