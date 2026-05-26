package io.horizontalsystems.bankwallet.modules.restorelocal

import io.horizontalsystems.bankwallet.entities.AccountType
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.OtherBackupViewItem
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsViewModel.WalletBackupViewItem

object RestoreLocalModule {

    data class UiState(
        val passphraseState: DataState.Error?,
        val showButtonSpinner: Boolean,
        val parseError: Exception?,
        val showSelectCoins: AccountType?,
        val manualBackup: Boolean,
        val restored: Boolean,
        val walletBackupViewItems: List<WalletBackupViewItem>,
        val otherBackupViewItems: List<OtherBackupViewItem>,
        val showBackupItems: Boolean,
        val hasSelection: Boolean,
    )
}
