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
        DisablingPin("term_disabling_pin"),
        PrivacyNotice("term_privacy_notice"),
        MonetizationDisclosure("term_monetization_disclosure"),
        OpenSource("term_open_source");

        val description: Int
            get() = when (this) {
                Backup -> R.string.SettingsTerms_Backup
                DisablingPin -> R.string.SettingsTerms_DisablingPin
                PrivacyNotice -> R.string.SettingsTerms_PrivacyNotice
                MonetizationDisclosure -> R.string.SettingsTerms_MonetizationDisclosure
                OpenSource -> R.string.SettingsTerms_OpenSource
            }
    }

    data class TermViewItem(val termType: TermType, val checked: Boolean)
}
