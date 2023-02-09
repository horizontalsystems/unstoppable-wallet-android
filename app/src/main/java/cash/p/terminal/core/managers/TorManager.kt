package cash.p.terminal.core.managers

import android.content.Context
import android.util.Log
import cash.p.terminal.core.AppLogger
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITorManager
import cash.p.terminal.modules.settings.security.tor.TorStatus
import io.horizontalsystems.tor.ConnectionStatus
import io.horizontalsystems.tor.Tor
import io.horizontalsystems.tor.TorKit
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

class TorManager(
    context: Context,
    val localStorage: ILocalStorage
) : ITorManager {

    private val logger = AppLogger("tor status")
    private val _torStatusFlow = MutableStateFlow(TorStatus.Connecting)

    override val torStatusFlow = _torStatusFlow
    override val torObservable = BehaviorSubject.create<TorStatus>()

    private val executorService = Executors.newCachedThreadPool()
    private val disposables = CompositeDisposable()
    private val kit: TorKit by lazy {
        TorKit(context)
    }

    init {
        if (localStorage.torEnabled) {
            start()
        }
    }

    override fun start() {
        disposables.add(kit.torInfoSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ torInfo ->
                    torObservable.onNext(getStatus(torInfo))
                    _torStatusFlow.update { getStatus(torInfo) }
                }, {
                    Log.e("TorManager", "Tor exception", it)
                })
        )
        executorService.execute {
            kit.startTor(false)
        }
    }

    override fun stop(): Single<Boolean> {
        return kit.stopTor()
    }

    override fun setTorAsEnabled() {
        localStorage.torEnabled = true
        logger.info("Tor enabled")
    }

    override fun setTorAsDisabled() {
        localStorage.torEnabled = false
        logger.info("Tor disabled")
    }

    override val isTorEnabled: Boolean
        get() = localStorage.torEnabled

    private fun getStatus(torinfo: Tor.Info): TorStatus {
        return when (torinfo.connection.status) {
            ConnectionStatus.CONNECTED -> TorStatus.Connected
            ConnectionStatus.CONNECTING -> TorStatus.Connecting
            ConnectionStatus.CLOSED -> TorStatus.Closed
            ConnectionStatus.FAILED -> TorStatus.Failed
        }
    }

}
