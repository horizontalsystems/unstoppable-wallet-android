package io.horizontalsystems.bankwallet.core.managers

import android.content.Context
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITorManager
import io.horizontalsystems.bankwallet.core.tor.ConnectionStatus
import io.horizontalsystems.bankwallet.core.tor.Tor
import io.horizontalsystems.bankwallet.core.tor.torcore.TorConstants
import io.horizontalsystems.bankwallet.core.tor.torcore.TorOperator
import io.horizontalsystems.bankwallet.core.tor.torutils.TorConnectionManager
import io.horizontalsystems.bankwallet.modules.settings.privacy.tor.TorStatus
import io.reactivex.Single
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors

class TorManager(context: Context, val localStorage: ILocalStorage) : ITorManager, TorOperator.Listener {

    private val logger = AppLogger("tor status")
    private val _torStatusFlow = MutableStateFlow(TorStatus.Closed)
    override val torStatusFlow = _torStatusFlow

    private val executorService = Executors.newCachedThreadPool()
    private val torOperator: TorOperator by lazy {
        TorOperator(Tor.Settings(context), this)
    }

    init {
        if (localStorage.torEnabled) {
            start()
        }
    }

    override fun start() {
        enableProxy()
        executorService.execute {
            torOperator.start()
        }
    }

    override fun stop(): Single<Boolean> {
        disableProxy()
        return torOperator.stop()
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

    override fun statusUpdate(torInfo: Tor.Info) {
        _torStatusFlow.update { getStatus(torInfo) }
    }

    private fun getStatus(torInfo: Tor.Info): TorStatus {
        return when (torInfo.connection.status) {
            ConnectionStatus.CONNECTED -> TorStatus.Connected
            ConnectionStatus.CONNECTING -> TorStatus.Connecting
            ConnectionStatus.CLOSED -> TorStatus.Closed
            ConnectionStatus.FAILED -> TorStatus.Failed
        }
    }

    private fun enableProxy() {
        TorConnectionManager.setSystemProxy(
            true,
            TorConstants.IP_LOCALHOST,
            TorConstants.HTTP_PROXY_PORT_DEFAULT,
            TorConstants.SOCKS_PROXY_PORT_DEFAULT
        )
    }

    private fun disableProxy() {
        TorConnectionManager.disableSystemProxy()
    }

}
