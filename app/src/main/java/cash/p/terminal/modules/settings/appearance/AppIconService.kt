package cash.p.terminal.modules.settings.appearance

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppIconService(private val localStorage: ILocalStorage) {
    private val appIcons = AppIcon.values().asList()

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.appIcon ?: AppIcon.Main, appIcons)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setAppIcon(appIcon: AppIcon) {
        localStorage.appIcon = appIcon

        _optionsFlow.update {
            Select(appIcon, appIcons)
        }
    }
}
