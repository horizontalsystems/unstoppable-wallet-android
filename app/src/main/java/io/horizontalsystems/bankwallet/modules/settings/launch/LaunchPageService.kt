package io.horizontalsystems.bankwallet.modules.settings.launch

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.settings.launch.LaunchPageModule.LaunchPageViewItem

class LaunchPageService(private val localStorage: ILocalStorage) {

    private val selectedOption: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    val options: List<LaunchPageViewItem>
        get() = LaunchPage.values().map { LaunchPageViewItem(it, it == selectedOption) }

    fun selectLaunchPage(launchPage: LaunchPage) {
        localStorage.launchPage = launchPage
    }

}
