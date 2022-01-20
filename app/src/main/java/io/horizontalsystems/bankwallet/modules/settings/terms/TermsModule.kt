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
        Academy("academy"),
        Backup("backup"),
        Owner("owner"),
        Recover("recover"),
        Phone("phone"),
        Root("root"),
        Bugs("bugs"),
        Pin("pin");

        val description: Int
            get() = when (this) {
                Academy -> R.string.SettingsTerms_TermAcademy
                Backup -> R.string.SettingsTerms_TermBackup
                Owner -> R.string.SettingsTerms_TermOwner
                Recover -> R.string.SettingsTerms_TermRecover
                Phone -> R.string.SettingsTerms_TermPhone
                Root -> R.string.SettingsTerms_TermRoot
                Bugs -> R.string.SettingsTerms_TermBugs
                Pin -> R.string.Settings_TermsPin
            }
    }

    data class TermViewItem(val termType: TermType, val checked: Boolean)
}
