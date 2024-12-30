package cash.p.terminal.modules.settings.language

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.managers.LanguageManager
import cash.p.terminal.core.stats.StatEvent
import cash.p.terminal.core.stats.StatPage
import cash.p.terminal.core.stats.stat
import cash.p.terminal.strings.helpers.LocaleType

class LanguageSettingsViewModel(
    private val languageManager: LanguageManager,
    private val localStorage: ILocalStorage
) : ViewModel() {

    val languageItems = cash.p.terminal.strings.helpers.LocaleType.values().map {
        LanguageViewItem(
            it,
            languageManager.getName(it.tag),
            languageManager.getNativeName(it.tag),
            it.icon,
            currentLocaleTag == it.tag
        )
    }

    private var currentLocaleTag: String
        get() = languageManager.currentLocaleTag
        set(value) {
            languageManager.currentLocaleTag = value
        }

    var closeScreen by mutableStateOf(false)
        private set

    var reloadApp by mutableStateOf(false)
        private set

    fun onSelectLocale(localeType: cash.p.terminal.strings.helpers.LocaleType) {
        if (localeType.tag == currentLocaleTag) {
            closeScreen = true
        } else {
            localStorage.relaunchBySettingChange = true
            currentLocaleTag = localeType.tag
            reloadApp = true

            stat(page = StatPage.Language, event = StatEvent.SwitchLanguage(localeType.tag))
        }
    }

}
