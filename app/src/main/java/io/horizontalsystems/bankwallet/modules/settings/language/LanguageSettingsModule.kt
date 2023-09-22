package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.core.helpers.LocaleType

object LanguageSettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSettingsViewModel(App.languageManager, App.localStorage) as T
        }
    }

}

val LocaleType.icon: Int
    get() {
        return when(this) {
            LocaleType.de -> R.drawable.icon_32_flag_germany
            LocaleType.en -> R.drawable.icon_32_flag_england
            LocaleType.es -> R.drawable.icon_32_flag_spain
            LocaleType.pt_br -> R.drawable.icon_32_flag_brazil
            LocaleType.fa -> R.drawable.icon_32_flag_iran
            LocaleType.fr -> R.drawable.icon_32_flag_france
            LocaleType.ko -> R.drawable.icon_32_flag_korea
            LocaleType.ru -> R.drawable.icon_32_flag_russia
            LocaleType.tr -> R.drawable.icon_32_flag_turkey
            LocaleType.zh -> R.drawable.icon_32_flag_china
        }
    }

data class LanguageViewItem(
    val localeType: LocaleType,
    val name: String,
    val nativeName: String,
    val icon: Int,
    var current: Boolean,
)
