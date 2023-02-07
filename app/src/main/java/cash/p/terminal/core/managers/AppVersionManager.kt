package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.entities.AppVersion
import io.horizontalsystems.core.ISystemInfoManager
import java.util.*

class AppVersionManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage
) {

    fun storeAppVersion() {
        val versions = localStorage.appVersions.toMutableList()
        val lastVersion = versions.lastOrNull()

        if (lastVersion == null || lastVersion.version != systemInfoManager.appVersion) {
            versions.add(AppVersion(systemInfoManager.appVersion, Date().time))
            localStorage.appVersions = versions
        }
    }

}
