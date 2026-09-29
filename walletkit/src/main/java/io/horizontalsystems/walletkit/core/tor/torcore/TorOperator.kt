package io.horizontalsystems.walletkit.core.tor.torcore

import com.jaredrummler.android.shell.Shell
import io.horizontalsystems.walletkit.core.tor.ConnectionStatus
import io.horizontalsystems.walletkit.core.tor.EntityStatus
import io.horizontalsystems.walletkit.core.tor.Tor
import io.horizontalsystems.walletkit.core.tor.torutils.ProcessUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.logging.Level
import java.util.logging.Logger

class TorOperator(private val torSettings: Tor.Settings, private val listener: Listener) : TorControl.Listener {

    interface Listener {
        fun statusUpdate(torInfo: Tor.Info)
    }

    private val logger = Logger.getLogger("TorOperator")
    val torInfo = Tor.Info(Tor.Connection())

    private var torControl: TorControl? = null
    private lateinit var resManager: TorResourceManager
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    fun start() {

        try {
            resManager = TorResourceManager(torSettings)
            val fileTorBin = resManager.installResources()
            val success = fileTorBin != null && fileTorBin.canExecute()

            if (success) {

                torInfo.isInstalled = true
                eventMonitor(torInfo = torInfo, msg = "Tor install success.")

                //-----------------------------
                killTorProcess()
                //-----------------------------

                // A control port file left by a previous run would point at a port nobody listens on
                resManager.fileTorControlPort.delete()

                if (runTorShellCmd(resManager.fileTor, resManager.fileTorrcCustom)) {

                    eventMonitor(msg = "Successfully verified config")

                    // Wait for control file creation -> Replace this implementation with RX.
                    //-----------------------------
                    Thread.sleep(100)
                    //-----------------------------

                    torControl = TorControl(
                        resManager.fileTorControlPort,
                        torSettings.appDataDir,
                        this,
                        torInfo
                    )

                    torInfo.status = EntityStatus.RUNNING
                    eventMonitor(torInfo = torInfo, msg = "Tor started successfully")

                    torControl?.let { it ->
                        coroutineScope.launch {
                            try {
                                it.initConnection(20).collect { torConnection ->
                                    torInfo.connection = torConnection
                                }
                            } catch (e: Throwable) {
                                torInfo.processId = -1
                                torInfo.connection.status = ConnectionStatus.FAILED
                                listener.statusUpdate(torInfo)
                            }
                        }
                    }
                } else {
                    throw IllegalStateException("Tor process did not start")
                }
            } else {
                throw FileNotFoundException("Error!!! Tor.so file notfound.")
            }

        } catch (e: java.lang.Exception) {
            torInfo.processId = -1
            torInfo.connection.status = ConnectionStatus.FAILED
            listener.statusUpdate(torInfo)

            eventMonitor(torInfo = torInfo, msg = "Error starting Tor")
            eventMonitor(msg = e.message.toString())
        }

    }

    override fun statusUpdate(torInfo: Tor.Info) {
        listener.statusUpdate(torInfo)
    }

    suspend fun stop(): Boolean {
        return killAllDaemons()
    }

    fun disableNetwork() {
        try {
            torControl?.setNetworkEnabled(false)
        } catch (e: Exception) {
            eventMonitor(msg = "Failed to disable Tor network: " + e.localizedMessage)
        }
    }

    // Blocks until Tor carries traffic again; the status goes through Connecting so the proxy
    // points at the reopened ports and waiting requests get a fresh attempt on Connected
    fun reconnect(reset: Boolean) {
        val control = torControl ?: return

        torInfo.connection.status = ConnectionStatus.CONNECTING
        listener.statusUpdate(torInfo)

        val connected = try {
            if (reset) control.setNetworkEnabled(false)
            control.setNetworkEnabled(true)
            listener.statusUpdate(torInfo)
            control.awaitBuiltCircuit(RECONNECT_TIMEOUT_MILLIS)
        } catch (e: Exception) {
            eventMonitor(msg = "Failed to reconnect Tor: " + e.localizedMessage)
            false
        }

        torInfo.connection.status = if (connected) ConnectionStatus.CONNECTED else ConnectionStatus.FAILED
        eventMonitor(torInfo, msg = if (connected) "Tor reconnected" else "Tor failed to reconnect")
    }

    fun newIdentity(): Boolean {
        return torControl?.newIdentity() ?: false
    }

    private fun eventMonitor(torInfo: Tor.Info? = null, logLevel: Level = Level.SEVERE, msg: String? = null) {

        msg?.let {
            logger.log(logLevel, msg)
        }

        torInfo?.let {
            it.statusMessage = msg
            listener.statusUpdate(it)
        }
    }

    @Throws(java.lang.Exception::class)
    private suspend fun killAllDaemons(): Boolean = withContext(Dispatchers.IO) {
        try {
            var result = torControl?.shutdownTor() ?: false

            if (!result) {
                result = killTorProcess()
            }

            torInfo.status = EntityStatus.STOPPED

            eventMonitor(torInfo, Level.INFO, "Tor stopped")
            result
        } catch (e: java.lang.Exception) {
            eventMonitor(torInfo, Level.SEVERE, "Tor stopped, but with errors:${e.localizedMessage}")
            throw e
        }
    }

    private fun killTorProcess(): Boolean {
        try {
            ProcessUtils.killProcess(resManager.fileTor) // this is -HUP
            return true
        } catch (e: Exception) {
            return false
        }
    }

    @Throws(Exception::class)
    private fun runTorShellCmd(fileTor: File, fileTorrc: File): Boolean {
        val appCacheHome: File = torSettings.appDataDir

        if (!fileTorrc.exists()) {
            eventMonitor(msg = "torrc not installed: " + fileTorrc.canonicalPath)
            return false
        }
        val torCmdString = (fileTor.canonicalPath
                + " DataDirectory " + appCacheHome.canonicalPath
                + " --defaults-torrc " + fileTorrc)

        var exitCode: Int

        exitCode = try {
            exec("$torCmdString --verify-config")
        } catch (e: Exception) {
            eventMonitor(msg = "Tor configuration did not verify: " + e.message + e)
            return false
        }

        if (exitCode != 0) {
            eventMonitor(msg = "Tor configuration did not verify:$exitCode")
            return false
        }

        exitCode = try {
            exec(torCmdString)
        } catch (e: Exception) {
            eventMonitor(msg = "Tor was unable to start: " + e.message + e)
            return false
        }

        if (exitCode != 0) {
            eventMonitor(msg = "Tor did not start. Exit:$exitCode")
            return false
        }

        return true
    }

    @Throws(Exception::class)
    private fun exec(cmd: String): Int {
        val shellResult = Shell.run(cmd)
        //  debug("CMD: " + cmd + "; SUCCESS=" + shellResult.isSuccessful());

        if (!shellResult.isSuccessful) {
            throw Exception(
                "Error: " + shellResult.exitCode + " ERR=" + shellResult.getStderr() + " OUT=" + shellResult.getStdout()
            )
        }

        eventMonitor(msg = "Result:$shellResult")

        return shellResult.exitCode
    }

    companion object {
        private const val RECONNECT_TIMEOUT_MILLIS = 60_000L
    }
}
