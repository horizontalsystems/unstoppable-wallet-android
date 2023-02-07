package cash.p.terminal.modules.settings.appearance

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.entities.LaunchPage
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LaunchScreenService(private val localStorage: ILocalStorage) {
    private val screens = LaunchPage.values().asList()

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.launchPage ?: LaunchPage.Auto, screens)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setLaunchScreen(launchPage: LaunchPage) {
        localStorage.launchPage = launchPage

        _optionsFlow.update {
            Select(launchPage, screens)
        }
    }
}
