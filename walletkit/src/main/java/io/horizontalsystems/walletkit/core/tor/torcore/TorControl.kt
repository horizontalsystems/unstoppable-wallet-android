package io.horizontalsystems.walletkit.core.tor.torcore

import io.horizontalsystems.walletkit.core.tor.ConnectionStatus
import io.horizontalsystems.walletkit.core.tor.Tor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import net.freehaven.tor.control.EventHandler
import net.freehaven.tor.control.TorControlConnection
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.net.Socket
import java.util.logging.Logger

class TorControl(
    private val fileControlPort: File,
    private val appCacheHome: File,
    private val listener: Listener,
    val torInfo: Tor.Info
) {

    interface Listener {
        fun statusUpdate(torInfo: Tor.Info)
    }

    private val logger = Logger.getLogger("TorControl")

    private val CONTROL_SOCKET_TIMEOUT = 60000
    private var controlConn: TorControlConnection? = null
    private var torEventHandler: TorEventHandler? = null
    private var torProcessId: Int = -1
    private val MAX_BOOTSTRAP_CHECK_TRIES = 120

    fun eventMonitor(torInfo: Tor.Info? = null, msg: String? = null) {
        msg?.let {
            logger.info(msg)
        }

        torInfo?.let {
            it.statusMessage = msg
            listener.statusUpdate(it)
        }
    }

    fun shutdownTor(): Boolean {

        if (!isConnectedToControl())
            return false

        try {
            controlConn?.let {
                it.shutdownTor("HALT")
                return true
            }
        } catch (e: java.lang.Exception) {
        }
        return false
    }

    private fun isConnectedToControl(): Boolean {
        return controlConn != null
    }

    fun initConnection(maxTries: Int): Flow<Tor.Connection> {

        torInfo.connection.status = ConnectionStatus.CONNECTING
        eventMonitor(torInfo)

        return createControlConn(maxTries)
            .map {
                configConnection(it, torInfo)
            }.catch {
                emit(failedConnection(it.localizedMessage))
            }
    }

    private fun failedConnection(reason: String?): Tor.Connection {
        controlConn = null
        torInfo.connection.processId = -1
        torInfo.connection.status = ConnectionStatus.FAILED
        eventMonitor(torInfo, msg = "Tor connection failed: $reason")
        return torInfo.connection
    }

    private fun createControlConn(maxTries: Int): Flow<TorControlConnection> {

        return flow {
            var attempt = 0

            while (controlConn == null && attempt++ < maxTries) {

                try {

                    val controlPort = getControlPort()

                    if (controlPort != -1) {

                        eventMonitor(msg = "Connecting to control port: $controlPort")

                        val torConnSocket = Socket(TorConstants.IP_LOCALHOST, controlPort)
                        torConnSocket.soTimeout = CONTROL_SOCKET_TIMEOUT

                        val conn = TorControlConnection(torConnSocket)
                        controlConn = conn

                        eventMonitor(msg = "SUCCESS connected to Tor control port.")
                        emit(conn)
                    }
                } catch (e: Exception) {
                    controlConn = null
                    eventMonitor(msg = "Error connecting to Tor local control port: " + e.localizedMessage)
                }

                // Wait for control file creation
                delay(300)
            }

            if (controlConn == null) {
                throw IllegalStateException("Tor control port did not come up")
            }
        }
    }

    private fun configConnection(conn: TorControlConnection, torInfo: Tor.Info): Tor.Connection {

        try {
            val fileCookie = File(appCacheHome, TorConstants.TOR_CONTROL_COOKIE)

            if (fileCookie.exists()) {
                val cookie = ByteArray(fileCookie.length().toInt())
                val fis = DataInputStream(FileInputStream(fileCookie))
                fis.read(cookie)
                fis.close()
                conn.authenticate(cookie)
                val torProcId = conn.getInfo("process/pid")

                torProcessId = torProcId.toInt()
                torInfo.connection.processId = torProcessId
                eventMonitor(torInfo, msg = "SUCCESS - started tor control processId:${torProcId}")

                torInfo.connection.proxySocksPort = listenerPort(conn, "net/listeners/socks")
                torInfo.connection.proxyHttpPort = listenerPort(conn, "net/listeners/httptunnel")

                torEventHandler = TorEventHandler(this)
                torEventHandler?.let {
                    addEventHandler(conn, it)
                }

                Thread {
                    onBootstrapped(torInfo)
                }.start()

                return torInfo.connection

            } else {
                return failedConnection("Tor authentication cookie does not exist")
            }
        } catch (e: Exception) {
            return failedConnection("Error configuring Tor connection: " + e.localizedMessage)
        }
    }

    // Tor answers with a quoted "127.0.0.1:43215" per listener; the ports are chosen by Tor
    // ("auto" in torrc), so they are only known once the control connection is up
    private fun listenerPort(conn: TorControlConnection, key: String): String {
        val listeners = conn.getInfo(key)
        return Regex(":(\\d+)").find(listeners)?.groupValues?.get(1)
            ?: throw IllegalStateException("Tor reported no listener for $key: $listeners")
    }

    // DisableNetwork closes the SOCKS and HTTP listeners along with the relay connections, and
    // with "auto" ports they reopen on new ones, so the ports are read again on every enable
    @Throws(IOException::class)
    fun setNetworkEnabled(enabled: Boolean) {
        val conn = controlConn ?: throw IOException("Not connected to Tor control port")
        conn.setConf("DisableNetwork", if (enabled) "0" else "1")
        if (enabled) {
            torInfo.connection.proxySocksPort = listenerPort(conn, "net/listeners/socks")
            torInfo.connection.proxyHttpPort = listenerPort(conn, "net/listeners/httptunnel")
        } else {
            torInfo.connection.proxySocksPort = null
            torInfo.connection.proxyHttpPort = null
        }
    }

    // Disabling the network closes every circuit, so any built one proves Tor reaches relays again
    fun awaitBuiltCircuit(timeoutMillis: Long): Boolean {
        val conn = controlConn ?: return false
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            try {
                val circuits = conn.getInfo("circuit-status")
                if (circuits.lineSequence().any { it.contains(" BUILT ") }) {
                    return true
                }
            } catch (e: IOException) {
                return false
            }
            Thread.sleep(500)
        }
        return false
    }

    fun newIdentity(): Boolean {
        return try {
            controlConn?.signal("NEWNYM")
            true
        } catch (e: IOException) {
            false
        }
    }

    @Synchronized
    fun onBootstrapped(torInfo: Tor.Info) {
        if (torInfo.connection.status != ConnectionStatus.CONNECTED) {

            eventMonitor(msg = "Starting Bootstrap status checking job ...")

            var isSuccess: Int
            var tries = 1

            do {
                isSuccess = getBootStatus()
                Thread.sleep(900)
                tries++

            } while (isSuccess == 0 && tries <= MAX_BOOTSTRAP_CHECK_TRIES)


            if (isSuccess == 1) {
                torInfo.connection.status = ConnectionStatus.CONNECTED
                eventMonitor(torInfo, msg = "Tor Bootstrapped 100%")
            } else if (isSuccess == -1 || tries >= MAX_BOOTSTRAP_CHECK_TRIES) {
                // if max tries exceeds then shutdown tor.
                torInfo.connection.status = ConnectionStatus.FAILED
                shutdownTor()
                eventMonitor(torInfo)
            }
        }
    }

    @Synchronized
    fun getBootStatus(): Int {

        controlConn?.let {

            try {
                val phase: String? = it.getInfo("status/bootstrap-phase")
                eventMonitor(msg = "Boot status:${phase}")

                if (phase != null && phase.contains("PROGRESS=100"))
                    return 1
                else
                    return 0

            } catch (e: IOException) {
                eventMonitor(msg = "Control connection is not responding properly to getInfo:${e}")
            }
        }

        return -1
    }

    private fun getControlPort(): Int {
        var result = -1

        try {
            if (fileControlPort.exists()) {
                eventMonitor(msg = "Reading control port config file: " + fileControlPort.canonicalPath)
                val bufferedReader =
                    BufferedReader(FileReader(fileControlPort))
                val line = bufferedReader.readLine()
                if (line != null) {
                    val lineParts = line.split(":").toTypedArray()
                    result = lineParts[1].toInt()
                }
                bufferedReader.close()

            } else {
                eventMonitor(
                    msg = "Control Port config file does not yet exist (waiting for tor): "
                            + fileControlPort.canonicalPath
                )
            }
        } catch (e: FileNotFoundException) {
            eventMonitor(msg = "unable to get control port; file not found")
        } catch (e: java.lang.Exception) {
            eventMonitor(msg = "unable to read control port config file")
        }

        return result
    }

    @Throws(java.lang.Exception::class)
    private fun addEventHandler(conn: TorControlConnection, torEventHandler: TorEventHandler) {
        eventMonitor(msg = "adding control port event handler")

        conn.let {
            it.setEventHandler(torEventHandler)
            it.setEvents(listOf("ORCONN", "CIRC", "NOTICE", "WARN", "ERR", "BW"))

            eventMonitor(msg = "SUCCESS added control port event handler")
        }
    }

    inner class TorEventHandler(private var torControl: TorControl) : EventHandler {

        override fun streamStatus(status: String?, streamID: String?, target: String?) {
        }

        override fun bandwidthUsed(read: Long, written: Long) {
            //logger.info("BandwidthUsed:${read},${written}")
        }

        // A single relay connection failing is routine while Tor bootstraps; the bootstrap
        // check in onBootstrapped decides whether Tor as a whole failed
        override fun orConnStatus(status: String?, orName: String?) {
        }

        override fun newDescriptors(orList: MutableList<String>?) {
        }

        override fun unrecognized(type: String?, msg: String?) {
        }

        override fun circuitStatus(status: String?, circID: String?, path: String?) {
        }

        override fun message(severity: String?, msg: String?) {
        }
    }
}
