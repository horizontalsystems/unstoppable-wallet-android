package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object LanguageSettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSettingsViewModel(App.languageManager, App.localStorage) as T
        }
    }

    enum class LocaleType(@DrawableRes val icon: Int) {
        de(R.drawable.icon_24_flags_germany),
        en(R.drawable.icon_24_flags_england),
        es(R.drawable.icon_24_flags_spain),
        fa(R.drawable.icon_24_flags_iran),
        fr(R.drawable.icon_24_flags_france),
        ko(R.drawable.icon_24_flags_korea),
        ru(R.drawable.icon_24_flags_russia),
        tr(R.drawable.icon_24_flags_turkey),
        zh(R.drawable.icon_24_flags_china),
    }
}

data class LanguageViewItem(
    val localeType: LanguageSettingsModule.LocaleType,
    val name: String,
    val nativeName: String,
    val icon: Int,
    var current: Boolean,
)
