package io.horizontalsystems.bankwallet.core.tor.torcore

import android.util.Log
import io.horizontalsystems.bankwallet.core.tor.Tor
import io.horizontalsystems.bankwallet.core.tor.torutils.FileUtils
import io.horizontalsystems.bankwallet.core.tor.torutils.NativeLoader
import io.horizontalsystems.bankwallet.core.tor.torutils.NetworkUtils
import java.io.*
import java.util.concurrent.TimeoutException
import java.util.logging.Logger
import java.util.zip.ZipInputStream

class TorResourceManager(private val torSettings: Tor.Settings) {

    private val logger = Logger.getLogger("TorResourceManager")

    lateinit var fileTor: File
    lateinit var fileTorrcCustom: File
    lateinit var fileTorControlPort: File
    private lateinit var fileTorrc: File

    @Throws(IOException::class, TimeoutException::class)
    fun installResources(): File? {

        if (!torSettings.appFilesDir.exists())
            torSettings.appFilesDir.mkdirs()

        if (!torSettings.appDataDir.exists())
            torSettings.appDataDir.mkdirs()

        fileTorControlPort = File(torSettings.appFilesDir, TorConstants.TOR_CONTROL_PORT_FILE)

        installGeoIP()

        fileTorrc = assetToFile(
            TorConstants.COMMON_ASSET_KEY + TorConstants.TORRC_ASSET_KEY,
            TorConstants.TORRC_ASSET_KEY, false, false
        )

        updateTorrcCustomFile()?.let {
            fileTorrcCustom = it
        }

        fileTor = File(torSettings.appNativeDir, TorConstants.TOR_ASSET_KEY + ".so")

        if (fileTor.exists()) {
            if (fileTor.canExecute())
                return fileTor
            else {
                FileUtils.setExecutable(fileTor)
                if (fileTor.canExecute())
                    return fileTor
            }

            val insStream: InputStream = FileInputStream(fileTor)
            streamToFile(insStream, fileTor, false, true)
            FileUtils.setExecutable(fileTor)

            if (fileTor.exists() && fileTor.canExecute())
                return fileTor

            //it exists but we can't execute it, so copy it to a new path
            return NativeLoader.loadNativeBinary(
                torSettings.appNativeDir, torSettings.appSourceDir, TorConstants.TOR_ASSET_KEY,
                File(torSettings.appFilesDir, TorConstants.TOR_ASSET_KEY)
            )?.let {

                if (it.exists())
                    FileUtils.setExecutable(fileTor)

                if (fileTor.exists() && fileTor.canExecute()) {
                    fileTor = it
                    fileTor
                } else
                    null
            }
        } else {
            logger.severe("Error!!! File:${fileTor} not found !!!")
        }
        return null
    }

    @Throws(IOException::class, TimeoutException::class)
    private fun updateTorrcCustomFile(): File? {

        val extraLines = StringBuffer()

        extraLines.append("\n")
        extraLines.append("RunAsDaemon 1").append('\n')
        extraLines.append("AvoidDiskWrites 1").append('\n')
        extraLines.append("ControlPortWriteToFile ").append(fileTorControlPort.absolutePath).append('\n')
        extraLines.append("ControlPort Auto").append('\n')
        extraLines.append("SOCKSPort ").append(checkPortOrAuto(TorConstants.SOCKS_PROXY_PORT_DEFAULT)).append('\n')
        extraLines.append("ReducedConnectionPadding 1").append('\n')
        extraLines.append("ReducedCircuitPadding 1").append('\n')
        extraLines.append("SafeSocks 0").append('\n')
        extraLines.append("TestSocks 0").append('\n')
        extraLines.append("TransPort 0").append('\n')
        extraLines.append("HTTPTunnelPort ").append(checkPortOrAuto(TorConstants.HTTP_PROXY_PORT_DEFAULT)).append('\n')
        extraLines.append("DNSPort ").append(checkPortOrAuto(TorConstants.TOR_DNS_PORT_DEFAULT)).append('\n')
        extraLines.append("CookieAuthentication 1").append('\n')
        extraLines.append("DisableNetwork 0").append('\n')

        val fileTorRcCustom = File(fileTorrc.getAbsolutePath() + ".custom")
        val success = updateTorConfigCustom(fileTorRcCustom, extraLines.toString())

        return if (success && fileTorRcCustom.exists()) {
            fileTorRcCustom
        } else null
    }

    private fun checkPortOrAuto(portString: String): String {

        if (!portString.toLowerCase().contentEquals("auto")) {
            var isPortUsed = true
            var port = portString.toInt()

            while (isPortUsed) {
                isPortUsed = NetworkUtils.isPortOpen("127.0.0.1", port, 500)
                if (isPortUsed) //the specified port is not available, so let Tor find one instead
                    port++
            }

            return port.toString() + ""
        }

        return portString
    }

    @Throws(IOException::class, FileNotFoundException::class, TimeoutException::class)
    fun updateTorConfigCustom(fileTorRcCustom: File, extraLines: String?): Boolean {
        if (fileTorRcCustom.exists()) {
            fileTorRcCustom.delete()
            Log.d("torResources", "deleting existing torrc.custom")
        } else
            fileTorRcCustom.createNewFile()

        val fos = FileOutputStream(fileTorRcCustom, false)
        val ps = PrintStream(fos)
        ps.print(extraLines)
        ps.close()

        return true
    }

    /*
     * Extract the Tor binary from the APK file using ZIP
     */
    @Throws(IOException::class)
    private fun installGeoIP(): Boolean {

        assetToFile(TorConstants.COMMON_ASSET_KEY + TorConstants.GEOIP_ASSET_KEY, TorConstants.GEOIP_ASSET_KEY)
        assetToFile(TorConstants.COMMON_ASSET_KEY + TorConstants.GEOIP6_ASSET_KEY, TorConstants.GEOIP6_ASSET_KEY)

        return true
    }

    /*
     * Reads file from assetPath/assetKey writes it to the install folder
     */
    @Throws(IOException::class)
    private fun assetToFile(
        assetPath: String, assetKey: String, isZipped: Boolean = false,
        isExecutable: Boolean = false
    ): File {

        val inpStream = torSettings.context.assets.open(assetPath)
        val outFile = File(torSettings.appFilesDir, assetKey)
        streamToFile(inpStream, outFile, false, isZipped)

        if (isExecutable) {
            FileUtils.setExecutable(outFile)
        }

        return outFile
    }

    /*
    * Write the inputstream contents to the file
    */
    @Throws(IOException::class)
    private fun streamToFile(stm: InputStream, outFile: File, append: Boolean, zip: Boolean): Boolean {

        var inpStream = stm
        val buffer = ByteArray(TorConstants.FILE_WRITE_BUFFER_SIZE)
        var bytecount: Int
        val stmOut: OutputStream = FileOutputStream(outFile.absolutePath, append)
        var zis: ZipInputStream? = null

        if (zip) {
            zis = ZipInputStream(stm)
            zis.nextEntry
            inpStream = zis
        }

        while (inpStream.read(buffer).also { bytecount = it } > 0) {
            stmOut.write(buffer, 0, bytecount)
        }
        stmOut.close()
        inpStream.close()
        zis?.close()

        return true
    }

}
