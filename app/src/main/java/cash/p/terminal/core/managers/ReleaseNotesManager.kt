package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import io.horizontalsystems.core.ISystemInfoManager

class ReleaseNotesManager(
        private val systemInfoManager: ISystemInfoManager,
        private val localStorage: ILocalStorage
) {

    private val currentAppVersion: String by lazy {
        systemInfoManager.appVersion
    }

    fun shouldShowChangeLog(): Boolean {
        val shownForVersion = localStorage.changelogShownForAppVersion

        if (shownForVersion != null) {
            return Version(currentAppVersion) > Version(shownForVersion)
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

    //compare only first two numbers, which stands for Major and Minor versions
    override fun compareTo(other: Version): Int {
        val maxSize = maxOf(splitted.size, other.splitted.size)
        for (i in 0 until maxSize) {
            val compare = splitted.getOrElse(i) { 0 }.compareTo(other.splitted.getOrElse(i) { 0 })
            if (compare != 0)
                return compare
        }
        return 0
    }
}
