package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.BackupSection
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import kotlinx.serialization.Serializable

@Serializable
data class BackupLocalPage(val account: Account? = null) : HSPage() {

    @Composable
    override fun GetContent(navigation: HSNavigation) {
        if (account != null) {
            LocalBackupPasswordScreen(
                backupType = BackupType.SingleWalletBackup(account.id),
                onBackClick = {
                    navigation.removeLastOrNull()
                },
                onFinish = {
                    navigation.removeLastOrNull()
                }
            )
        } else {
            SelectBackupItemsScreen(
                onNextClick = { accountIdsList, sections ->
                    navigation.add(LocalBackupPasswordPage(accountIdsList, sections))
                },
                onBackClick = {
                    navigation.removeLastOrNull()
                }
            )
        }
    }
}

@Serializable
data class LocalBackupPasswordPage(
    val accountIds: List<String>,
    val sections: Set<BackupSection>
) : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        LocalBackupPasswordScreen(
            backupType = BackupType.FullBackup(accountIds, sections),
            onBackClick = {
                navigation.removeLastOrNull()
            },
            onFinish = {
                navigation.removeLastOrNull()
            }
        )
    }
}
