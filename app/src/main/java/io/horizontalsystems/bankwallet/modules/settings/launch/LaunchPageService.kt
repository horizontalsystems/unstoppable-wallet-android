package io.horizontalsystems.bankwallet.modules.settings.launch

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.LaunchPage

class LaunchPageService(private val localStorage: ILocalStorage) {

    val selectedOption: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    val options = LaunchPage.values().asList()

    fun selectLaunchPage(launchPage: LaunchPage) {
        localStorage.launchPage = launchPage
    }

}
