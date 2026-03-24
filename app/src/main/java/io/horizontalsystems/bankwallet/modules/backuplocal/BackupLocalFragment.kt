package io.horizontalsystems.bankwallet.modules.backuplocal

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.password.BackupType
import io.horizontalsystems.bankwallet.modules.backuplocal.password.LocalBackupPasswordScreen
import io.horizontalsystems.bankwallet.modules.backuplocal.terms.LocalBackupTermsScreen
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen

class BackupLocalFragment(val account: Account? = null) : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavBackStack<HSScreen>) {
        if (account != null) {
            SingleWalletBackupNavHost(navController, account.id)
        } else {
            FullBackupNavHost(fragmentNavController = navController)
        }
    }
}

@Composable
private fun FullBackupNavHost(fragmentNavController: NavBackStack<HSScreen>) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "select_backup_items",
    ) {
        composable("select_backup_items") {
            SelectBackupItemsScreen(
                onNextClick = { accountIdsList ->
                    val accountIds = if (accountIdsList.isNotEmpty()) "?accountIds=" + accountIdsList.joinToString(separator = ",") else ""
                    navController.navigate("terms_page$accountIds")
                },
                onBackClick = {
                    fragmentNavController.removeLastOrNull()
                }
            )
        }

        composablePage(
            route = "terms_page?accountIds={accountIds}",
            arguments = listOf(navArgument("accountIds") { nullable = true })
        ) { backStackEntry ->
            val accountIds = backStackEntry.arguments?.getString("accountIds")
            LocalBackupTermsScreen(
                onTermsAccepted = {
                    navController.navigate("password_page${if (accountIds != null) "?accountIds=$accountIds" else ""}")
                },
                onBackClick = {
                    navController.popBackStack()
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
                    fragmentNavController.removeLastOrNull()
                }
            )
        }
    }
}

@Composable
private fun SingleWalletBackupNavHost(fragmentNavController: NavBackStack<HSScreen>, accountId: String) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "terms_page",
    ) {
        composable("terms_page") {
            LocalBackupTermsScreen(
                onTermsAccepted = {
                    navController.navigate("password_page")
                },
                onBackClick = {
                    fragmentNavController.removeLastOrNull()
                }
            )
        }
        composablePage("password_page") {
            LocalBackupPasswordScreen(
                backupType = BackupType.SingleWalletBackup(accountId),
                onBackClick = {
                    navController.popBackStack()
                },
                onFinish = {
                    fragmentNavController.removeLastOrNull()
                }
            )
        }
    }
}
