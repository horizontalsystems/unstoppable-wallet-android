package io.horizontalsystems.bankwallet.modules.theme

import androidx.appcompat.app.AppCompatDelegate
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ThemeService(private val localStorage: ILocalStorage) {
    private val themes by lazy { ThemeType.values().toList() }

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.currentTheme, themes)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setThemeType(themeType: ThemeType) {
        localStorage.currentTheme = themeType

        _optionsFlow.update {
            Select(themeType, themes)
        }

        val nightMode = when (themeType) {
            ThemeType.Light -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeType.Dark -> AppCompatDelegate.MODE_NIGHT_YES
            ThemeType.System -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
