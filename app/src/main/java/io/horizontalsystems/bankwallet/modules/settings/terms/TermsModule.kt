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
        PrivateKeys("term_privatekeys"),
        DisablingPin("term_disablingpin"),
        JailBraking("term_jailbraking"),
        Bugs("term_bugs");

        val description: Int
            get() = when (this) {
                Backup -> R.string.SettingsTerms_Backup
                PrivateKeys -> R.string.SettingsTerms_PrivateKeys
                DisablingPin -> R.string.SettingsTerms_DisablingPin
                JailBraking -> R.string.SettingsTerms_JailBraking
                Bugs -> R.string.SettingsTerms_Bugs
            }
    }

    data class TermViewItem(val termType: TermType, val checked: Boolean)
}
