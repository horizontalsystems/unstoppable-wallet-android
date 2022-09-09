package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.ILanguageManager

class LanguageSettingsViewModel(
    private val languageManager: ILanguageManager,
    private val localStorage: ILocalStorage,
) : ViewModel() {

    val languageItems = LanguageSettingsModule.LocaleType.values().map {
        LanguageViewItem(
            it,
            languageManager.getName(it.name),
            languageManager.getNativeName(it.name),
            it.icon,
            currentLocale == it.name
        )
    }

    private var currentLocale: String
        get() = languageManager.currentLanguage
        set(value) {
            languageManager.currentLanguage = value
        }

    var closeScreen by mutableStateOf(false)
        private set

    var reloadApp by mutableStateOf(false)
        private set

    fun onSelectLocale(localeType: LanguageSettingsModule.LocaleType) {
        if (localeType.name == currentLocale) {
            closeScreen = true
        } else {
            localStorage.relaunchBySettingChange = true
            currentLocale = localeType.name
            reloadApp = true
        }
    }

}
