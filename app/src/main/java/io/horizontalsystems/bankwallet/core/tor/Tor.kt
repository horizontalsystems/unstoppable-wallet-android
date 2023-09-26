package io.horizontalsystems.bankwallet.core.tor

import android.app.Application
import android.content.Context
import io.horizontalsystems.bankwallet.core.tor.torcore.TorConstants
import java.io.File

enum class EntityStatus(val processId: Int) {
    STARTING(-1),
    RUNNING(1),
    STOPPED(0);

    companion object {

        fun getByProcessId(procId: Int): EntityStatus {
            return values()
                .find { it.processId == procId } ?: RUNNING
        }
    }

    override fun toString(): String {
        return this.name
    }
}

enum class ConnectionStatus {

    CLOSED,
    CONNECTING,
    CONNECTED,
    FAILED;

    companion object {

        fun getByName(typName: String): ConnectionStatus {
            return values()
                .find { it.name.contentEquals(typName.toUpperCase()) } ?: CLOSED
        }
    }

    override fun toString(): String {
        return this.name
    }
}

object Tor {

    class Info(var connection: Connection) {

        var processId: Int
            get() = connection.processId
            set(value) {
                connection.processId = value
            }

        var isInstalled: Boolean = false
        var statusMessage: String? = null

        var status: EntityStatus
            get() = EntityStatus.getByProcessId(processId)
            set(value) {
                processId = value.processId

                if (value == EntityStatus.STOPPED)
                    connection.status = ConnectionStatus.CLOSED
            }
    }

    class Connection(processIdArg: Int = -1) {

        var processId: Int = processIdArg
            set(value) {
                if (processId > 0)
                    status = ConnectionStatus.CONNECTING
                field = value
            }

        var proxyHost = TorConstants.IP_LOCALHOST
        var proxySocksPort = TorConstants.SOCKS_PROXY_PORT_DEFAULT
        var proxyHttpPort = TorConstants.HTTP_PROXY_PORT_DEFAULT
        var status: ConnectionStatus = ConnectionStatus.CLOSED
    }

    class Settings(var context: Context) {
        var appFilesDir: File = context.filesDir
        var appDataDir: File = context.getDir(TorConstants.DIRECTORY_TOR_DATA, Application.MODE_PRIVATE)
        var appNativeDir: File = File(context.applicationInfo.nativeLibraryDir)
        var appSourceDir: File = File(context.applicationInfo.sourceDir)
        var useBridges: Boolean = false
    }

}
