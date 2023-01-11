package io.horizontalsystems.bankwallet.modules.settings.language

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

    enum class LocaleType(val tag: String, val icon: Int) {
        de("de", R.drawable.icon_32_flag_germany),
        en("en", R.drawable.icon_32_flag_england),
        es("es", R.drawable.icon_32_flag_spain),
        pt_br("pt-BR", R.drawable.icon_32_flag_brazil),
        fa("fa", R.drawable.icon_32_flag_iran),
        fr("fr", R.drawable.icon_32_flag_france),
        ko("ko", R.drawable.icon_32_flag_korea),
        ru("ru", R.drawable.icon_32_flag_russia),
        tr("tr", R.drawable.icon_32_flag_turkey),
        zh("zh", R.drawable.icon_32_flag_china);
    }
}

data class LanguageViewItem(
    val localeType: LanguageSettingsModule.LocaleType,
    val name: String,
    val nativeName: String,
    val icon: Int,
    var current: Boolean,
)
