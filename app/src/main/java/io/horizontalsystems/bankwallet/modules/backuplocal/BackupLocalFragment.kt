package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen

class BackupLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        val account = navController.getInput<Account>()
        if (account != null) {
            SingleWalletBackupNavHost(navController, account.id)
        } else {
            FullBackupNavHost(fragmentNavController = navController)
        }
    }
}

@Composable
private fun FullBackupNavHost(fragmentNavController: NavController) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "select_backup_items",
    ) {
        composable("select_backup_items") {
            SelectBackupItemsScreen(
                onNextClick = { accountIdsList ->
                    val accountIds = if (accountIdsList.isNotEmpty()) "?accountIds=" + accountIdsList.joinToString(separator = ",") else ""
                    navController.navigate("password_page?accountIds=$accountIds")
                },
                onBackClick = {
                    fragmentNavController.popBackStack()
                }
            )
        }

        composablePage(
            route = "password_page?accountIds={accountIds}",
            arguments = listOf(navArgument("accountIds") { nullable = true })
        ) { backStackEntry ->
            val accountIds = backStackEntry.arguments?.getString("accountIds")?.split(",") ?: listOf()
            LocalBackupPasswordScreen(
                backupType = BackupType.FullBackup(accountIds),
                onBackClick = {
                    navController.popBackStack()
                },
                onFinish = {
                    fragmentNavController.popBackStack()
                }
            )
        }
    }
}

@Composable
private fun SingleWalletBackupNavHost(fragmentNavController: NavController, accountId: String) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "password_page",
    ) {
        composablePage("password_page") {
            LocalBackupPasswordScreen(
                backupType = BackupType.SingleWalletBackup(accountId),
                onBackClick = {
                    navController.popBackStack()
                },
                onFinish = {
                    fragmentNavController.popBackStack()
                }
            )
        }
    }
}
