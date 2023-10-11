package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator

class BackupViewItemFactory {

    fun backupViewItems(backupItems: BackupItems): Pair<List<SelectBackupItemsViewModel.WalletBackupViewItem>, List<SelectBackupItemsViewModel.OtherBackupViewItem>> {

        val walletBackupViewItems = backupItems.accounts.map { account ->
            SelectBackupItemsViewModel.WalletBackupViewItem(
                account = account,
                name = account.name,
                type = account.type.detailedDescription,
                backupRequired = !account.hasAnyBackup,
                selected = true
            )
        }

        val otherBackupViewItems = buildList {
            backupItems.watchWallets?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = Translator.getString(R.string.BackupManager_WatchWallets), value = "$it"))
            }
            backupItems.watchlist?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = Translator.getString(R.string.BackupManager_Watchlist), value = "$it"))
            }
            backupItems.contacts?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = Translator.getString(R.string.Contacts), value = "$it"))
            }
            backupItems.customRpc?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = Translator.getString(R.string.BackupManager_CustomRpc), value = "$it"))
            }
            add(
                SelectBackupItemsViewModel.OtherBackupViewItem(
                    title = Translator.getString(R.string.BackupManager_AppSettingsTitle),
                    subtitle = Translator.getString(R.string.BackupManager_AppSettingsDescription)
                )
            )
        }

        return Pair(walletBackupViewItems, otherBackupViewItems)
    }

}
