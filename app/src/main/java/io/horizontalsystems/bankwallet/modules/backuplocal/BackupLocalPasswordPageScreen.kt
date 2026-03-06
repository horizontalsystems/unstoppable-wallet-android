package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.ResultEventBus
import io.horizontalsystems.bankwallet.modules.nav3.removeLastUntil
import kotlinx.serialization.Serializable

@Serializable
data class BackupLocalPasswordPageScreen(val backupType: BackupType) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>,
        resultBus: ResultEventBus
    ) {
        LocalBackupPasswordScreen(
            backupType = backupType,
            onBackClick = {
                backStack.removeLastOrNull()
            },
            onFinish = {
                backStack.removeLastUntil(BackupLocalScreen::class, true)
            }
        )
    }
}