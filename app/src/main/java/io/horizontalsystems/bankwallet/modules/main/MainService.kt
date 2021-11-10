package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.utils.RootUtil
import io.horizontalsystems.bankwallet.entities.LaunchPage

class MainService(
    private val rootUtil: RootUtil,
    private val localStorage: ILocalStorage
) {

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
