package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.ISystemInfoManager

class DonationShowManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
) {

    private val currentAppVersion: String by lazy {
        systemInfoManager.appVersion
    }

    fun shouldShow(): Boolean {
        //show only for FDroid builds
//        val isFDroidBuild = BuildConfig.FDROID_BUILD
//        if (!isFDroidBuild) {
//            return false
//        }
        //todo in version 0.43 remove changelogShownForAppVersion and update logic
        val prevVersion = localStorage.changelogShownForAppVersion
        val donateAppVersion = localStorage.donateAppVersion
        if (donateAppVersion == null && prevVersion == "0.42.3" && currentAppVersion == "0.42.4") {
            return true
        }

        //its fresh install, no need to show
        updateDonateAppVersion()
        return false
    }

    fun updateDonateAppVersion() {
        localStorage.donateAppVersion = systemInfoManager.appVersion
    }

}
