package io.horizontalsystems.bankwallet.modules.settings.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.entities.LaunchPage
import io.horizontalsystems.bankwallet.modules.settings.theme.ThemeType
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val launchScreenService: LaunchScreenService,
    private val themeService: ThemeService
) : ViewModel() {
    private var launchScreenOptions = getLaunchScreenOptions()
    private var themeOptions = themeService.optionsFlow.value

    var uiState by mutableStateOf(
        AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            themeOptions = themeOptions
        )
    )

    init {
        viewModelScope.launch {
            themeService.optionsFlow
                .collect {
                    handleUpdatedThemeOptions(it)
                }
        }
    }

    private fun handleUpdatedThemeOptions(themeOptions: Select<ThemeType>) {
        this.themeOptions = themeOptions
        emitState()
    }

    fun onEnterLaunchPage(launchPage: LaunchPage) {
        launchScreenService.selectLaunchPage(launchPage)
        launchScreenOptions = getLaunchScreenOptions()
        emitState()
    }

    private fun emitState() {
        uiState = AppearanceUIState(
            launchScreenOptions = launchScreenOptions,
            themeOptions = themeOptions
        )
    }

    private fun getLaunchScreenOptions(): Select<LaunchPage> {
        return Select(launchScreenService.selectedOption, launchScreenService.options)
    }

    fun onEnterTheme(themeType: ThemeType) {
        themeService.setThemeType(themeType)
    }

}

data class AppearanceUIState(
    val launchScreenOptions: Select<LaunchPage>,
    val themeOptions: Select<ThemeType>
)
