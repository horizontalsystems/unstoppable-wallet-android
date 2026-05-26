package io.horizontalsystems.bankwallet.modules.manageaccount

import io.horizontalsystems.bankwallet.modules.balance.HeaderNote

object ManageAccountModule {

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
