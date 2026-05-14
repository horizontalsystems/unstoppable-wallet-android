package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupSection
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class BackupLocalFragment(val account: Account? = null) : HSScreen() {

    @Composable
    override fun GetContent(navController: HSNavigation) {
        if (account != null) {
            LocalBackupPasswordScreen(
                backupType = BackupType.SingleWalletBackup(account.id),
                onBackClick = {
                    navController.removeLastOrNull()
                },
                onFinish = {
                    navController.removeLastOrNull()
                }
            )
        } else {
            SelectBackupItemsScreen(
                onNextClick = { accountIdsList, sections ->
                    navController.add(LocalBackupPasswordPage(accountIdsList, sections))
                },
                onBackClick = {
                    navController.removeLastOrNull()
                }
            )
        }
    }
}

@Serializable
data class LocalBackupPasswordPage(
    val accountIds: List<String>,
    val sections: Set<BackupSection>
) : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        LocalBackupPasswordScreen(
            backupType = BackupType.FullBackup(accountIds, sections),
            onBackClick = {
                navController.removeLastOrNull()
            },
            onFinish = {
                navController.removeLastOrNull()
            }
        )
    }
}
