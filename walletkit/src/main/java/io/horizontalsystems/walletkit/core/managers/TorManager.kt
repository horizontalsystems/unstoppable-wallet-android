package io.horizontalsystems.walletkit.core.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.core.tor.ConnectionStatus
import io.horizontalsystems.walletkit.core.tor.Tor
import io.horizontalsystems.walletkit.core.tor.torcore.TorConstants
import io.horizontalsystems.walletkit.core.tor.torcore.TorOperator
import io.horizontalsystems.walletkit.core.tor.torutils.TorConnectionManager
import io.horizontalsystems.walletkit.modules.settings.privacy.tor.TorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class TorManager(
    private val context: Context,
    val localStorage: ILocalStorage,
    private val backgroundManager: BackgroundManager,
) : ITorManager, TorOperator.Listener {

    private val logger = AppLogger("tor status")
    private val _torStatusFlow = MutableStateFlow(TorStatus.Closed)
    override val torStatusFlow = _torStatusFlow

    private val executorService = Executors.newCachedThreadPool()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Sleeping, waking and resetting block on Tor, and must not overlap
    private val networkExecutor = Executors.newSingleThreadExecutor()
    private var asleep = false
    private var currentNetwork: Network? = null
    private val torOperator: TorOperator by lazy {
        TorOperator(Tor.Settings(context), this)
    }

    init {
        if (localStorage.torEnabled) {
            start()
            observeAppState()
            observeNetworkChanges()
        }
    }

    // Android cuts network access for apps in the background, Tor included, while Tor keeps
    // its dead relay connections and still reports itself connected. Requests made on return
    // would hang on those connections and end in sync errors, so Tor sleeps in the background
    // and on return reconnects before it is reported connected again, which refreshes the kits.
    private fun observeAppState() {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                networkExecutor.execute {
                    when (state) {
                        BackgroundManagerState.EnterBackground -> sleep()
                        BackgroundManagerState.EnterForeground -> wake()
                    }
                }
            }
        }
    }

    private fun sleep() {
        // A Tor still starting up is left alone; its own bootstrap check decides its state
        if (asleep || _torStatusFlow.value != TorStatus.Connected) return
        asleep = true
        blockProxy()
        torOperator.disableNetwork()
    }

    private fun wake() {
        if (!asleep) return
        asleep = false
        torOperator.reconnect(reset = false)
    }

    // Relay connections made over the previous network are dead after a switch, so Tor
    // reconnects right away instead of waiting for each of them to time out
    private fun observeNetworkChanges() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onDefaultNetworkChanged(network)
            }
        })
    }

    private fun onDefaultNetworkChanged(network: Network) {
        networkExecutor.execute {
            val previous = currentNetwork
            currentNetwork = network
            if (previous == null || previous == network) return@execute
            if (asleep || _torStatusFlow.value != TorStatus.Connected) return@execute

            logger.info("Default network changed, reconnecting Tor")
            torOperator.reconnect(reset = true)
        }
    }

    override fun start() {
        blockProxy()
        executorService.execute {
            torOperator.start()
        }
    }

    override suspend fun stop(): Boolean {
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
        val status = getStatus(torInfo)
        if (localStorage.torEnabled) {
            val socksPort = torInfo.connection.proxySocksPort
            val httpPort = torInfo.connection.proxyHttpPort
            // Tor accepts requests on its ports before its circuits are ready and holds them
            // until they are, so the ports are used as soon as Tor reports them
            val running = status == TorStatus.Connecting || status == TorStatus.Connected
            if (running && socksPort != null && httpPort != null) {
                enableProxy(httpPort, socksPort)
            } else {
                blockProxy()
            }
        }
        _torStatusFlow.update { status }
    }

    private fun getStatus(torInfo: Tor.Info): TorStatus {
        return when (torInfo.connection.status) {
            ConnectionStatus.CONNECTED -> TorStatus.Connected
            ConnectionStatus.CONNECTING -> TorStatus.Connecting
            ConnectionStatus.CLOSED -> TorStatus.Closed
            ConnectionStatus.FAILED -> TorStatus.Failed
        }
    }

    private fun enableProxy(httpPort: String, socksPort: String) {
        TorConnectionManager.setSystemProxy(true, TorConstants.IP_LOCALHOST, httpPort, socksPort)
    }

    // Traffic that reads the proxy settings fails instead of going out directly while Tor is
    // starting, retrying or has failed
    private fun blockProxy() {
        enableProxy(TorConstants.BLOCKED_PROXY_PORT, TorConstants.BLOCKED_PROXY_PORT)
    }

    private fun disableProxy() {
        TorConnectionManager.disableSystemProxy()
    }

}
