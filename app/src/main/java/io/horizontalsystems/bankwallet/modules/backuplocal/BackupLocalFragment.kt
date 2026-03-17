package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import kotlinx.serialization.Serializable

@Serializable
data class BackupLocalScreen(val account: Account? = null) : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        val screen = if (account != null) {
            BackupLocalTermsPageScreen(BackupType.SingleWalletBackup(account.id))
        } else {
            BackupLocalSelectItemsScreen
        }

        screen.GetContent(backStack)
    }
}

class BackupLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
    }
}
