package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import android.util.Log
import io.horizontalsystems.bankwallet.core.IBlockchainSettingsManager
import io.horizontalsystems.bankwallet.core.ICommunicationSettingsManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.CommunicationMode
import io.horizontalsystems.bankwallet.entities.CommunicationSetting
import io.horizontalsystems.tor.ConnectionStatus
import io.horizontalsystems.tor.Tor
import io.horizontalsystems.tor.TorKit
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject

class TorManager(
        context: Context,
        val localStorage: ILocalStorage,
        val blockchainSettingsManager: IBlockchainSettingsManager)
    : ITorManager {

    interface Listener{
        fun onStatusChange(torStatus: TorStatus)
    }

    override val torObservable = BehaviorSubject.create<TorStatus>()
    override val isTorNotificationEnabled: Boolean
        get() = kit.isNotificationEnabled

    private val disposables = CompositeDisposable()
    private var listener: Listener? = null
    private val kit: TorKit by lazy {
        TorKit(context)
    }

    init {
        if (localStorage.torEnabled) {
            start()
        }
    }

    override fun start() {

        // Actions taken before tor connection starts
        doBeforeTorConnected()

        disposables.add(kit.torInfoSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    listener?.onStatusChange(getStatus(it))
                    torObservable.onNext(getStatus(it))
                }, {
                    Log.e("TorManager", "Tor exception", it)
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

    override fun setListener(listener: Listener) {
        this.listener = listener
    }

    override val isTorEnabled: Boolean
        get() = localStorage.torEnabled

    private fun getStatus(torinfo: Tor.Info): TorStatus {
        return when (torinfo.connection.status) {
            ConnectionStatus.CONNECTED ->TorStatus.Connected
            ConnectionStatus.CONNECTING ->TorStatus.Connecting
            ConnectionStatus.CLOSED ->TorStatus.Closed
            ConnectionStatus.FAILED ->TorStatus.Failed
        }
    }

    private fun doBeforeTorConnected(){
        // Set Ethereum to Infura communication mode (In3 doesnt connect through proxy)
        blockchainSettingsManager.saveSetting(CommunicationSetting(CoinType.Ethereum,CommunicationMode.Infura))
    }
}

enum class TorStatus(val value: String) {
    Connected("Connected"),
    Connecting("Connecting"),
    Closed("Closed"),
    Failed("Failed");
}
