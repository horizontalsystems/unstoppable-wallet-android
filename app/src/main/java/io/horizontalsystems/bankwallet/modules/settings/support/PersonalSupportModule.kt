package io.horizontalsystems.bankwallet.modules.settings.support

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object PersonalSupportModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PersonalSupportViewModel(App.marketKit, App.localStorage) as T
        }
    }

    data class UiState(
        val contactName: String,
        val showSuccess: Boolean = false,
        val showError: Boolean = false,
        val showSpinner: Boolean = false,
        val buttonEnabled: Boolean = false,
        val showRequestForm: Boolean = false,
    )
}
