package cash.p.terminal.modules.main

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.utils.RootUtil
import cash.p.terminal.entities.LaunchPage

class MainService(
    private val rootUtil: RootUtil,
    private val localStorage: ILocalStorage
) {
    val marketsTabEnabledFlow by localStorage::marketsTabEnabledFlow

    val isDeviceRooted: Boolean
        get() = rootUtil.isRooted

    val ignoreRootCheck: Boolean
        get() = localStorage.ignoreRootedDeviceWarning

    val launchPage: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    var currentMainTab: MainModule.MainTab?
        get() = localStorage.mainTab
        set(value) {
            localStorage.mainTab = value
        }

    var relaunchBySettingChange: Boolean
        get() = localStorage.relaunchBySettingChange
        set(value) {
            localStorage.relaunchBySettingChange = value
        }

}
