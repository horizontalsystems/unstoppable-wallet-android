package io.horizontalsystems.bankwallet.modules.settings.terms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App

object TermsModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TermsViewModel(App.termsManager) as T
        }
    }

    enum class TermType(val key: String) {
        Backup("term_backup"),
        DisablingPin("term_disablingpin");

        val description: Int
            get() = when (this) {
                Backup -> R.string.SettingsTerms_Backup
                DisablingPin -> R.string.SettingsTerms_DisablingPin
            }
    }

    data class TermViewItem(val termType: TermType, val checked: Boolean)
}
