package cash.p.terminal.modules.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import cash.p.terminal.strings.helpers.LocaleType

object LanguageSettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSettingsViewModel(App.languageManager, App.localStorage) as T
        }
    }

}

val cash.p.terminal.strings.helpers.LocaleType.icon: Int
    get() {
        return when(this) {
            cash.p.terminal.strings.helpers.LocaleType.de -> R.drawable.icon_32_flag_germany
            cash.p.terminal.strings.helpers.LocaleType.en -> R.drawable.icon_32_flag_england
            cash.p.terminal.strings.helpers.LocaleType.es -> R.drawable.icon_32_flag_spain
            cash.p.terminal.strings.helpers.LocaleType.pt_br -> R.drawable.icon_32_flag_brazil
            cash.p.terminal.strings.helpers.LocaleType.fa -> R.drawable.icon_32_flag_iran
            cash.p.terminal.strings.helpers.LocaleType.fr -> R.drawable.icon_32_flag_france
            cash.p.terminal.strings.helpers.LocaleType.ko -> R.drawable.icon_32_flag_korea
            cash.p.terminal.strings.helpers.LocaleType.ru -> R.drawable.icon_32_flag_russia
            cash.p.terminal.strings.helpers.LocaleType.tr -> R.drawable.icon_32_flag_turkey
            cash.p.terminal.strings.helpers.LocaleType.zh -> R.drawable.icon_32_flag_china
        }
    }

data class LanguageViewItem(
    val localeType: cash.p.terminal.strings.helpers.LocaleType,
    val name: String,
    val nativeName: String,
    val icon: Int,
    var current: Boolean,
)
