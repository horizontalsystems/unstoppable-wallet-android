package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.IAppStatusManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ISystemInfoManager
import java.util.*
import kotlin.collections.LinkedHashMap

class AppStatusManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage
) : IAppStatusManager {

    override val status: LinkedHashMap<String, Any>
        get() {
            val status = LinkedHashMap<String, Any>()

            val app = LinkedHashMap<String, Any>()
            app["Current Time"] = Date()
            app["App Version"] = systemInfoManager.appVersion
            app["Device Model"] = systemInfoManager.deviceModel
            app["OS Version"] = systemInfoManager.osVersion

            status["App Info"] = app

            val versions = LinkedHashMap<String, Date>()
            localStorage.appVersions.sortedBy { it.timestamp }.forEach { version ->
                versions[version.version] = Date(version.timestamp)
            }
            status["Version History"] = versions

            val bitcoinStatus = LinkedHashMap<String, Any>()

            bitcoinStatus["Synced Until"] = "Jun 2, 2019"
            val peer1 = LinkedHashMap<String, Any>()
            peer1["Status"] = "active"
            peer1["IP Address"] = "192.12.34.1"
            val testInfo = LinkedHashMap<String, Any>()
            testInfo["testInfo1"] = "value1"
            testInfo["testInfo2"] = "value2"
            peer1["Test Info"] = testInfo

            bitcoinStatus["Peer 1"] = peer1


            val blockchainStatus = LinkedHashMap<String, Any>()
            blockchainStatus["Bitcoin"] = bitcoinStatus
            blockchainStatus["Bitcoin Cash"] = bitcoinStatus

            status["Blockchain Status"] = blockchainStatus

            return status
        }

}
