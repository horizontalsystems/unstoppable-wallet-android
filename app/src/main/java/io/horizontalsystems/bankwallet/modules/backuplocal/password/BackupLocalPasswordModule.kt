package io.horizontalsystems.bankwallet.modules.backuplocal.password

import io.horizontalsystems.bankwallet.entities.DataState

object BackupLocalPasswordModule {

    data class UiState(
        val backupName: String,
        val passphraseState: DataState.Error?,
        val passphraseConfirmState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val backupJson: String?,
        val closeScreen: Boolean,
        val error: String?
    )
}