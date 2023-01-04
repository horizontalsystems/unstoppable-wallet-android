package io.horizontalsystems.bankwallet.modules.settings.language

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import java.util.*

object LanguageSettingsModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LanguageSettingsViewModel(App.languageManager) as T
        }
    }
}

data class LanguageViewItem(
    val locale: Locale,
    val name: String,
    val nativeName: String,
    val icon: Int,
    var current: Boolean,
)
