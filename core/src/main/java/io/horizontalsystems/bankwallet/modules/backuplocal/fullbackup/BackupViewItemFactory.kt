package io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup

import io.horizontalsystems.core.R
import io.horizontalsystems.bankwallet.core.providers.Translator

class BackupViewItemFactory {

    fun backupViewItems(backupItems: BackupItems): Pair<List<SelectBackupItemsViewModel.WalletBackupViewItem>, List<SelectBackupItemsViewModel.OtherBackupViewItem>> {

        val walletBackupViewItems = backupItems.accounts.map { account ->
            SelectBackupItemsViewModel.WalletBackupViewItem(
                account = account,
                name = account.name,
                type = if (account.isWatchAccount)
                    Translator.getString(R.string.WatchWallet)
                else
                    account.type.detailedDescription,
                backupRequired = !account.hasAnyBackup,
                selected = true
            )
        }

        val otherBackupViewItems = buildList {
            backupItems.contacts?.let {
                add(
                    SelectBackupItemsViewModel.OtherBackupViewItem(
                        section = BackupSection.Contacts,
                        title = Translator.getString(R.string.Contacts),
                        value = Translator.getString(R.string.BackupManager_CountSavedAddresses, it)
                    )
                )
            }
            backupItems.watchlist?.let {
                add(
                    SelectBackupItemsViewModel.OtherBackupViewItem(
                        section = BackupSection.Favourites,
                        title = Translator.getString(R.string.BackupManager_Watchlist),
                        value = Translator.getString(R.string.BackupManager_CountAssets, it)
                    )
                )
            }
            backupItems.customRpc?.let {
                add(
                    SelectBackupItemsViewModel.OtherBackupViewItem(
                        section = BackupSection.CustomRpc,
                        title = Translator.getString(R.string.BackupManager_CustomRpc),
                        value = Translator.getString(R.string.BackupManager_CountNetworks, it)
                    )
                )
            }
            val showPreferences =
                backupItems.sections?.let { BackupSection.Preferences in it } ?: true
            if (showPreferences) {
                add(
                    SelectBackupItemsViewModel.OtherBackupViewItem(
                        section = BackupSection.Preferences,
                        title = Translator.getString(R.string.BackupManager_AppSettingsTitle),
                        subtitle = Translator.getString(R.string.BackupManager_AppSettingsDescription)
                    )
                )
            }
        }

        return Pair(walletBackupViewItems, otherBackupViewItems)
    }

}
