package io.horizontalsystems.bankwallet.core.tor.torcore

import android.text.TextUtils
import io.horizontalsystems.bankwallet.core.tor.ConnectionStatus
import io.horizontalsystems.bankwallet.core.tor.Tor
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import net.freehaven.tor.control.EventHandler
import net.freehaven.tor.control.TorControlConnection
import java.io.*
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
    private val MAX_BOOTSTRAP_CHECK_TRIES = 60

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

    fun initConnection(maxTries: Int): Observable<Tor.Connection> {

        torInfo.connection.status = ConnectionStatus.CONNECTING
        eventMonitor(torInfo)

        return createControlConn(maxTries)
            .subscribeOn(Schedulers.io())
            .map {
                configConnection(it, torInfo)
            }.onErrorReturn {
                Tor.Connection(-1)
            }
    }

    private fun createControlConn(maxTries: Int): Observable<TorControlConnection> {

        return Observable.create { emitter ->
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
                        emitter.onNext(conn)
                    }
                } catch (e: Exception) {
                    controlConn = null
                    torInfo.connection.processId = -1
                    torInfo.connection.status = ConnectionStatus.FAILED

                    eventMonitor(torInfo, msg = "Error connecting to Tor local control port: " + e.localizedMessage)
                    emitter.tryOnError(e)
                }

                // Wait for control file creation -> Replace this implementation with RX.
                //-----------------------------
                Thread.sleep(300)
                //-----------------------------
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

                torEventHandler = TorEventHandler(this)
                torEventHandler?.let {
                    addEventHandler(conn, it)
                }

                return torInfo.connection

            } else {
                eventMonitor(msg = "Tor authentication cookie does not exist yet")
            }
        } catch (e: Exception) {

            controlConn = null
            torInfo.connection.processId = -1
            torInfo.connection.status = ConnectionStatus.FAILED
            eventMonitor(torInfo, msg = "Error configuring Tor connection: " + e.localizedMessage)
        }

        return Tor.Connection(-1)
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

        override fun orConnStatus(status: String?, orName: String?) {
            status?.let {

                if (TextUtils.equals(status, "CONNECTED")) {

                    Thread(Runnable {
                        torControl.onBootstrapped(torControl.torInfo)
                    }).start()

                } else if (TextUtils.equals(status, "FAILED")) {
                    torControl.torInfo.connection.status = ConnectionStatus.FAILED
                    torControl.eventMonitor(torControl.torInfo)
                }
            }

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
