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
        WeNotHoldingYourAssets("term_we_not_holding_your_assets"),
        MonetizationDisclosure("term_monetization_disclosure"),
        TransactionsIrreversible("term_transactions_irreversible"),
        OpenSource("term_open_source"),
        LegalObligations("term_legal_obligations");

        val description: Int
            get() = when (this) {
                Backup -> R.string.SettingsTerms_Backup
                DisablingPin -> R.string.SettingsTerms_DisablingPin
                PrivacyNotice -> R.string.SettingsTerms_PrivacyNotice
                WeNotHoldingYourAssets -> R.string.SettingsTerms_WeNotHoldingYourAssets
                MonetizationDisclosure -> R.string.SettingsTerms_MonetizationDisclosure
                TransactionsIrreversible -> R.string.SettingsTerms_TransactionsIrreversible
                OpenSource -> R.string.SettingsTerms_OpenSource
                LegalObligations -> R.string.SettingsTerms_LegalObligations
            }
    }

    data class TermViewItem(val termType: TermType, val checked: Boolean)
}
