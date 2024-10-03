package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.providers.AppConfigProvider
import io.horizontalsystems.core.ISystemInfoManager

class ReleaseNotesManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage,
        appConfigProvider: AppConfigProvider
) {

    private val currentAppVersion: String by lazy {
        systemInfoManager.appVersion
    }

    val releaseNotesUrl =
        "${appConfigProvider.releaseNotesUrl}${Version(currentAppVersion).versionForUrl}"

    fun shouldShowChangeLog(): Boolean {
        val shownForVersion = localStorage.changelogShownForAppVersion

        if (shownForVersion != null) {
            return if (Version(currentAppVersion) > Version(shownForVersion)) {
                true
            } else {
                false
            }
        }

        //its fresh install, no need to show
        updateShownAppVersion()
        return false
    }

    fun updateShownAppVersion() {
        localStorage.changelogShownForAppVersion = systemInfoManager.appVersion
    }

}

class Version(private val value: String) : Comparable<Version> {
    //Semantic Version: Major/Minor/Patch

    private val splitted by lazy { value.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList() }

    val versionForUrl by lazy {
        //release notes available for version with 0 as patch number
        //e.g. 0.23.0
        if (splitted.size >= 3) {
            "${splitted[0]}.${splitted[1]}.0"
        } else {
            value
        }
    }


    //compare only first two numbers, which stands for Major and Minor versions
    override fun compareTo(other: Version): Int {
        val maxSize = maxOf(splitted.size, other.splitted.size)
        for (i in 0 until maxSize) {
            if (i > 1) {
                // ignore last number, which stands for patch version
                return 0
            }
            val compare = splitted.getOrElse(i) { 0 }.compareTo(other.splitted.getOrElse(i) { 0 })
            if (compare != 0)
                return compare
        }
        return 0
    }
}
