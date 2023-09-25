package io.horizontalsystems.bankwallet.core.tor.torcore

import com.jaredrummler.android.shell.Shell
import io.horizontalsystems.bankwallet.core.tor.ConnectionStatus
import io.horizontalsystems.bankwallet.core.tor.EntityStatus
import io.horizontalsystems.bankwallet.core.tor.Tor
import io.horizontalsystems.bankwallet.core.tor.torutils.ProcessUtils
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
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

                    torControl?.let {
                        it.initConnection(4)
                            .subscribe(
                                { torConnection ->
                                    torInfo.connection = torConnection
                                },
                                {
                                    torInfo.processId = -1
                                })

                    }
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

    fun stop(): Single<Boolean> {
        return killAllDaemons()
            .subscribeOn(Schedulers.io())
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
    private fun killAllDaemons(): Single<Boolean> {

        return Single.create { emitter ->

            try {
                var result = torControl?.shutdownTor() ?: false

                if (!result) {
                    result = killTorProcess()
                }

                torInfo.status = EntityStatus.STOPPED

                eventMonitor(torInfo, Level.INFO, "Tor stopped")
                emitter.onSuccess(result)

            } catch (e: java.lang.Exception) {
                eventMonitor(torInfo, Level.SEVERE, "Tor stopped, but with errors:${e.localizedMessage}")
                emitter.onError(e)
            }
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
            exec("$torCmdString --verify-config", true)
        } catch (e: Exception) {
            eventMonitor(msg = "Tor configuration did not verify: " + e.message + e)
            return false
        }

        if (exitCode != 0) {
            eventMonitor(msg = "Tor configuration did not verify:$exitCode")
            return false
        }

        exitCode = try {
            exec(torCmdString, true)
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
    private fun exec(cmd: String, wait: Boolean = false): Int {
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

}