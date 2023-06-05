package io.horizontalsystems.bankwallet.modules.manageaccount

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.HeaderNote

object ManageAccountModule {
    const val ACCOUNT_ID_KEY = "account_id_key"

    class Factory(private val accountId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManageAccountViewModel(accountId, App.accountManager) as T
        }
    }

    fun prepareParams(accountId: String) = bundleOf(ACCOUNT_ID_KEY to accountId)

    data class ViewState(
        val title: String,
        val newName: String,
        val canSave: Boolean,
        val closeScreen: Boolean,
        val headerNote: HeaderNote,
        val keyActions: List<KeyAction>,
        val backupActions: List<BackupItem>,
    )

    enum class KeyAction {
        RecoveryPhrase,
        PublicKeys,
        PrivateKeys,
    }

    sealed class BackupItem{
        class ManualBackup(val showAttention: Boolean, val completed: Boolean = false) : BackupItem()
        class LocalBackup(val showAttention: Boolean) : BackupItem()
        class InfoText(val textRes: Int) : BackupItem()
    }
}
