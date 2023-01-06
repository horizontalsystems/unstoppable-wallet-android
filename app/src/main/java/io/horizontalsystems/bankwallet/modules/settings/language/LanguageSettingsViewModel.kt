package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.managers.LanguageManager
import java.util.*

class LanguageSettingsViewModel(
    private val languageManager: LanguageManager
) : ViewModel() {

    val languageItems: List<LanguageViewItem>
        get() = LanguageManager.supportedLocales.map {
            LanguageViewItem(it.locale, it.name(languageManager.currentLocale), it.nativeName, it.icon, languageManager.currentLocale == it.locale)
        }

    var closeScreen by mutableStateOf(false)
        private set

    fun onSelectLocale(locale: Locale) {
        if (locale == languageManager.currentLocale) {
            closeScreen = true
        } else {
            languageManager.setLocale(locale)
        }
    }

}
