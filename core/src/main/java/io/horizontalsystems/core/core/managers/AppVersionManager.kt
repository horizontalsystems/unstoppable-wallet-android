package io.horizontalsystems.core.core.managers

import io.horizontalsystems.core.ISystemInfoManager
import io.horizontalsystems.core.core.ILocalStorage
import io.horizontalsystems.core.entities.AppVersion
import java.util.Date

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
