package com.quantum.wallet.bankwallet.modules.settings.appearance

import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.entities.LaunchPage
import com.quantum.wallet.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LaunchScreenService(private val localStorage: ILocalStorage) {
    private val screens by lazy { LaunchPage.entries }

    val selectedLaunchScreen: LaunchPage
        get() = localStorage.launchPage ?: LaunchPage.Auto

    private val _optionsFlow = MutableStateFlow(
        Select(selectedLaunchScreen, screens)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setLaunchScreen(launchPage: LaunchPage) {
        localStorage.launchPage = launchPage

        _optionsFlow.update {
            Select(launchPage, screens)
        }
    }
}
