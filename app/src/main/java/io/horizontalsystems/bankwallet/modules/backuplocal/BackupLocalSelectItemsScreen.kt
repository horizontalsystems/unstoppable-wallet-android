package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data object BackupLocalSelectItemsScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        SelectBackupItemsScreen(
            onNextClick = { accountIdsList ->
                backStack.add(BackupLocalTermsPageScreen(BackupType.FullBackup(accountIdsList)))
            },
            onBackClick = {
                backStack.removeLastOrNull()
            }
        )
    }
}