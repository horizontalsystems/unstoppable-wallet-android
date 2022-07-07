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

    private val localizations = "de,en,es,fa,fr,ko,ru,tr,zh"
    private val availableLanguages = localizations.split(",")

    val languageItems = availableLanguages.map {
        LanguageViewItem(
            it,
            languageManager.getName(it),
            languageManager.getNativeName(it),
            currentLanguage == it
        )
    }

    private var currentLanguage: String
        get() = languageManager.currentLanguage
        set(value) {
            languageManager.currentLanguage = value
        }

    var closeScreen by mutableStateOf(false)
        private set

    var reloadApp by mutableStateOf(false)
        private set

    fun onSelectLanguage(language: String) {
        if (language == currentLanguage) {
            closeScreen = true
        } else {
            localStorage.relaunchBySettingChange = true
            currentLanguage = language
            reloadApp = true
        }
    }

}
