package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.ISystemInfoManager

class ChangeLogsManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage
) {

    fun shouldShowChangeLog(): Boolean {
        val lastAppVersion = localStorage.lastAppVersion
        val currentAppVersion = systemInfoManager.appVersion

        if (lastAppVersion != null) {
            if (Version(currentAppVersion) > Version(lastAppVersion)) {
                localStorage.lastAppVersion = systemInfoManager.appVersion
                return true
            }
        } else if (Version(currentAppVersion).getMinorVersion() == 21) {
            //todo remove this check in version 22
            localStorage.lastAppVersion = systemInfoManager.appVersion
            return true
        }

        return false
    }

    fun getChangeLog(): String {
        return "file:///changelogs/changelog.md"
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
