package io.horizontalsystems.walletkit.core.managers

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.Network
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.horizontalsystems.walletkit.core.AppLogger
import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.core.ITorManager
import io.horizontalsystems.walletkit.modules.settings.privacy.tor.TorStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.freehaven.tor.control.TorControlConnection
import org.torproject.jni.TorService
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Runs Tor inside the app process through tor-android's [TorService] and routes the app's
 * traffic through its SOCKS port.
 *
 * Traffic is routed with the JVM proxy properties rather than per client: the default
 * ProxySelector reads them for every connection, so every OkHttp and HttpURLConnection client,
 * including those the kits build internally, goes through Tor. Only the SOCKS properties are
 * set, which the selector uses for http and https alike, so hostnames are resolved by Tor.
 * The Bitcoin and Monero kits read the same properties for their own sockets.
 */
class TorManager(
    private val context: Context,
    val localStorage: ILocalStorage,
    private val backgroundManager: BackgroundManager,
) : ITorManager {

    private val logger = AppLogger("tor status")
    private val _torStatusFlow = MutableStateFlow(TorStatus.Closed)
    override val torStatusFlow: StateFlow<TorStatus> = _torStatusFlow.asStateFlow()
    private val _bootstrapProgressFlow = MutableStateFlow<Int?>(null)
    override val bootstrapProgressFlow: StateFlow<Int?> = _bootstrapProgressFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Starting, sleeping, waking and resetting block on Tor, and must not overlap
    private val executor = Executors.newSingleThreadExecutor()

    private var service: TorService? = null
    private var bound = false
    private var stopping = false
    // TorService keeps its control connection after Tor exits, so it cannot tell whether Tor runs
    private var torExited = false
    private var asleep = false
    private var currentNetwork: Network? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val torService = (binder as TorService.LocalBinder).service
            executor.execute {
                service = torService
                connect()
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            executor.execute {
                service = null
                fail("Tor service disconnected")
            }
        }
    }

    // Tor exiting on its own, on an error, is only reported this way. OFF is not handled: a
    // service destroyed to start Tor again reports it after the new one has started.
    private val statusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(TorService.EXTRA_STATUS) == TorService.STATUS_STOPPING) {
                executor.execute {
                    torExited = true
                    if (!stopping) fail("Tor stopped")
                }
            }
        }
    }

    init {
        deleteLegacyFiles()
        if (localStorage.torEnabled) {
            ContextCompat.registerReceiver(
                context,
                statusReceiver,
                IntentFilter(TorService.ACTION_STATUS),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            start()
            observeAppState()
            observeNetworkChanges()
        }
    }

    override val isTorEnabled: Boolean
        get() = localStorage.torEnabled

    override fun setTorAsEnabled() {
        localStorage.torEnabled = true
        logger.info("Tor enabled")
    }

    override fun setTorAsDisabled() {
        localStorage.torEnabled = false
        logger.info("Tor disabled")
    }

    override fun start() {
        blockProxy()
        _torStatusFlow.value = TorStatus.Connecting

        executor.execute {
            val control = service?.torControlConnection
            if (control != null && !torExited) {
                // Tor is still running after a failure, so it only needs to reconnect
                reconnect(control, reset = true)
                return@execute
            }

            if (bound) {
                // Tor exited; unbinding destroys the service so binding creates a fresh one
                context.unbindService(serviceConnection)
                bound = false
                service = null
            }
            torExited = false
            writeTorrc()
            bound = context.bindService(Intent(context, TorService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
            if (!bound) fail("Cannot bind Tor service")
        }
    }

    override suspend fun stop(): Boolean = withContext(Dispatchers.IO) {
        executor.submit {
            stopping = true
            clearProxy()
            if (bound) {
                // The service shuts Tor down when it is destroyed
                context.unbindService(serviceConnection)
                bound = false
            }
            service = null
            _torStatusFlow.value = TorStatus.Closed
        }.get()
        true
    }

    // Some services refuse part of the Tor exits, and Tor keeps a circuit for up to 10 minutes,
    // so a kit that failed on one exit keeps failing until its circuit changes
    override suspend fun newCircuits() = withContext(Dispatchers.IO) {
        executor.submit {
            if (_torStatusFlow.value != TorStatus.Connected) return@submit
            try {
                service?.torControlConnection?.signal("NEWNYM")
            } catch (e: IOException) {
                Timber.w(e, "Failed to request new Tor circuits")
            }
        }.get()
        Unit
    }

    // Runs once TorService is bound: waits for its control connection and bootstrap, then routes
    // traffic to the SOCKS port and reports Connected, which refreshes the kits. Traffic is only
    // routed to Tor once it is ready: requests Tor held while bootstrapping would hit their own
    // timeouts after that refresh and leave the kits in errors nothing retries, while requests
    // refused before it fail at once and are retried by it.
    private fun connect() {
        val control = awaitControlConnection() ?: return fail("Tor control connection did not come up")

        try {
            // Bootstrap can report 100% before a single circuit exists
            val bootstrapped = awaitBootstrap(control) && awaitBuiltCircuit(control)
            _bootstrapProgressFlow.value = null
            if (bootstrapped) {
                enableProxy(socksPort(control))
                _torStatusFlow.value = TorStatus.Connected
                Timber.d("Tor bootstrapped")
            } else {
                fail("Tor bootstrap timed out")
            }
        } catch (e: IOException) {
            fail("Tor bootstrap failed: ${e.message}")
        }
    }

    // Blocks until Tor carries traffic again, routing it to Tor only then, as in connect().
    // Disabling the network closes the SOCKS listener as well and with an "auto" port it reopens
    // on a new one, so the port is read again.
    private fun reconnect(control: TorControlConnection, reset: Boolean) {
        blockProxy()
        _torStatusFlow.value = TorStatus.Connecting

        try {
            if (reset) control.setConf("DisableNetwork", "1")
            control.setConf("DisableNetwork", "0")
            if (awaitBuiltCircuit(control)) {
                enableProxy(socksPort(control))
                _torStatusFlow.value = TorStatus.Connected
                Timber.d("Tor reconnected")
            } else {
                fail("Tor did not reconnect")
            }
        } catch (e: IOException) {
            fail("Tor reconnect failed: ${e.message}")
        }
    }

    private fun fail(reason: String) {
        Timber.w(reason)
        _bootstrapProgressFlow.value = null
        blockProxy()
        _torStatusFlow.value = TorStatus.Failed
    }

    // Android cuts network access for apps in the background, Tor included, while Tor keeps its
    // dead relay connections and still reports itself connected. Requests made on return would
    // hang on those connections and end in sync errors, so Tor sleeps in the background and on
    // return reconnects before it is reported connected again.
    private fun observeAppState() {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                executor.execute {
                    when (state) {
                        BackgroundManagerState.EnterBackground -> sleep()
                        BackgroundManagerState.EnterForeground -> wake()
                    }
                }
            }
        }
    }

    private fun sleep() {
        // A Tor still starting up is left alone; its bootstrap decides its state
        if (asleep || _torStatusFlow.value != TorStatus.Connected) return
        val control = service?.torControlConnection ?: return

        asleep = true
        blockProxy()
        try {
            control.setConf("DisableNetwork", "1")
        } catch (e: IOException) {
            Timber.w(e, "Failed to disable Tor network")
        }
    }

    private fun wake() {
        if (!asleep) return
        asleep = false
        val control = service?.torControlConnection ?: return fail("Tor is gone after sleep")
        reconnect(control, reset = false)
    }

    // Relay connections made over the previous network are dead after a switch, so Tor
    // reconnects right away instead of waiting for each of them to time out
    private fun observeNetworkChanges() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                executor.execute { onDefaultNetworkChanged(network) }
            }
        })
    }

    private fun onDefaultNetworkChanged(network: Network) {
        val previous = currentNetwork
        currentNetwork = network
        if (previous == null || previous == network || asleep) return

        when (_torStatusFlow.value) {
            TorStatus.Connected -> {
                val control = service?.torControlConnection ?: return
                Timber.d("Default network changed, reconnecting Tor")
                reconnect(control, reset = true)
            }
            // A new network is the likeliest fix for a failed Tor, which nothing else retries
            TorStatus.Failed -> {
                Timber.d("Default network changed, starting failed Tor again")
                start()
            }
            else -> Unit
        }
    }

    private fun awaitControlConnection(): TorControlConnection? {
        repeat(CONTROL_WAIT_TRIES) {
            service?.torControlConnection?.let { return it }
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return null
    }

    private fun awaitBootstrap(control: TorControlConnection): Boolean {
        repeat(BOOTSTRAP_WAIT_TRIES) {
            val phase = control.getInfo("status/bootstrap-phase")
            val progress = Regex("PROGRESS=(\\d+)").find(phase)?.groupValues?.get(1)?.toInt()
            if (progress == 100) return true
            _bootstrapProgressFlow.value = progress
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    // After DisableNetwork every circuit is closed, so any built one proves Tor reaches relays again
    private fun awaitBuiltCircuit(control: TorControlConnection): Boolean {
        repeat(RECONNECT_WAIT_TRIES) {
            if (control.getInfo("circuit-status").lineSequence().any { it.contains(" BUILT ") }) return true
            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
        return false
    }

    // Tor answers with a quoted "127.0.0.1:43215"
    private fun socksPort(control: TorControlConnection): Int {
        val listeners = control.getInfo("net/listeners/socks")
        return Regex(":(\\d+)").find(listeners)?.groupValues?.get(1)?.toInt()
            ?: throw IOException("Tor reported no SOCKS listener: $listeners")
    }

    // TorService passes this file with -f, so its options override the defaults it writes
    private fun writeTorrc() {
        TorService.getTorrc(context).writeText(
            """
            SOCKSPort auto
            HTTPTunnelPort 0
            DNSPort 0
            TransPort 0
            SafeSocks 0
            TestSocks 0
            AvoidDiskWrites 1
            ReducedConnectionPadding 1
            ReducedCircuitPadding 1
            """.trimIndent()
        )
    }

    // Tor used to run as a separate executable with its own files; they are useless now
    private fun deleteLegacyFiles() {
        listOf("torrc", "torrc.custom", "control.txt", "geoip", "geoip6").forEach {
            File(context.filesDir, it).delete()
        }
        File(context.dataDir, "app_data").deleteRecursively()
    }

    private fun enableProxy(socksPort: Int) {
        System.setProperty("socksProxyHost", LOCALHOST)
        System.setProperty("socksProxyPort", socksPort.toString())
    }

    // Traffic that reads the proxy settings fails instead of going out directly while Tor is
    // starting, reconnecting, asleep or failed. Nothing listens on port 1 and an unprivileged
    // app cannot bind it.
    private fun blockProxy() {
        enableProxy(BLOCKED_PORT)
    }

    private fun clearProxy() {
        System.clearProperty("socksProxyHost")
        System.clearProperty("socksProxyPort")
    }

    companion object {
        // Right after Tor connects, the first circuits to each destination can take longer than
        // some kits wait, so kits that failed meanwhile get another attempt after this long
        const val WARM_UP_MILLIS = 20_000L

        private const val LOCALHOST = "127.0.0.1"
        private const val BLOCKED_PORT = 1
        private const val POLL_INTERVAL_MILLIS = 500L
        private const val CONTROL_WAIT_TRIES = 60 // 30 s
        private const val BOOTSTRAP_WAIT_TRIES = 240 // 2 min
        private const val RECONNECT_WAIT_TRIES = 120 // 1 min
    }
}
