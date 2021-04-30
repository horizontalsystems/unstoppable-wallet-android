package io.horizontalsystems.bankwallet.modules.main

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.utils.RootUtil

class MainService(
        private val rootUtil: RootUtil,
        private val localStorage: ILocalStorage
) {

    val isDeviceRooted: Boolean
        get() = rootUtil.isRooted

    val ignoreRootCheck: Boolean
        get() = localStorage.ignoreRootedDeviceWarning

}
