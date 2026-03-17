package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.terms.LocalBackupTermsScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class BackupLocalTermsPageScreen(val backupType: BackupType) : HSScreen() {
    @Composable
    override fun GetContent(backStack: NavBackStack<HSScreen>) {
        LocalBackupTermsScreen(
            onTermsAccepted = {
                backStack.add(BackupLocalPasswordPageScreen(backupType))
            },
            onBackClick = {
                backStack.removeLastOrNull()
            }
        )
    }
}