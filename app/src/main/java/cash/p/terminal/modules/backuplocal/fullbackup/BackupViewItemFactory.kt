package cash.p.terminal.modules.backuplocal.fullbackup

import cash.p.terminal.R

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
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = cash.p.terminal.strings.helpers.Translator.getString(R.string.BackupManager_WatchWallets), value = "$it"))
            }
            backupItems.watchlist?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = cash.p.terminal.strings.helpers.Translator.getString(R.string.BackupManager_Watchlist), value = "$it"))
            }
            backupItems.contacts?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = cash.p.terminal.strings.helpers.Translator.getString(R.string.Contacts), value = "$it"))
            }
            backupItems.customRpc?.let {
                add(SelectBackupItemsViewModel.OtherBackupViewItem(title = cash.p.terminal.strings.helpers.Translator.getString(R.string.BackupManager_CustomRpc), value = "$it"))
            }
            add(
                SelectBackupItemsViewModel.OtherBackupViewItem(
                    title = cash.p.terminal.strings.helpers.Translator.getString(R.string.BackupManager_AppSettingsTitle),
                    subtitle = cash.p.terminal.strings.helpers.Translator.getString(R.string.BackupManager_AppSettingsDescription)
                )
            )
        }

        return Pair(walletBackupViewItems, otherBackupViewItems)
    }

}
