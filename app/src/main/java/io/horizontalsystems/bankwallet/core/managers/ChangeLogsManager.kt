package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.ISystemInfoManager

class ChangeLogsManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage
) {

    fun getChangeLog(): String {
        return "file:///changelogs/changelog.md"
    }

    fun shouldShowChangeLog(): Boolean {
        val shownForVersion = localStorage.changelogShownForAppVersion
        val currentAppVersion = systemInfoManager.appVersion

        if (shownForVersion != null) {
            return if (Version(currentAppVersion) > Version(shownForVersion)) {
                updateShownAppVersion()
                true
            } else {
                false
            }
        }

        //todo remove this check in version 22
        if (Version(currentAppVersion).getMinorVersion() == 21) {
            val storedAppVersions = localStorage.appVersions
            storedAppVersions.reversed().forEach {
                val minorVersion = Version(it.version).getMinorVersion()
                if (minorVersion != null && minorVersion < 21) {
                    updateShownAppVersion()
                    return true
                }
            }
        }

        //its fresh install, no need to show
        updateShownAppVersion()
        return false
    }

    private fun updateShownAppVersion() {
        localStorage.changelogShownForAppVersion = systemInfoManager.appVersion
    }

}

class Version(private val value: String) : Comparable<Version> {
    //Semantic Version: Major/Minor/Patch

    private val splitted by lazy { value.split(".").map { it.toIntOrNull() ?: 0 }.toMutableList() }

    fun getMinorVersion(): Int? {
        return value.split(".").getOrNull(1)?.toIntOrNull()
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
