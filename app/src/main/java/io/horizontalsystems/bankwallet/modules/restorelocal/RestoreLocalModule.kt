package io.horizontalsystems.bankwallet.modules.restorelocal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState

object RestoreLocalModule {

    class Factory(private val backupJsonString: String?, private val fileName: String?) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RestoreLocalViewModel(backupJsonString, fileName, App.accountFactory) as T
        }
    }

    data class UiState(
        val passphraseState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val parseError: Exception?,
        val accountType: AccountType?,
        val manualBackup: Boolean
    )
}