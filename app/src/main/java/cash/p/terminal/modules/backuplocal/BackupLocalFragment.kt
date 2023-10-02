package cash.p.terminal.modules.backuplocal

import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cash.p.terminal.core.BaseComposeFragment
import cash.p.terminal.core.composablePage
import cash.p.terminal.modules.backuplocal.fullbackup.SelectBackupItemsScreen
import cash.p.terminal.modules.backuplocal.password.BackupType
import cash.p.terminal.modules.backuplocal.password.LocalBackupPasswordScreen
import cash.p.terminal.modules.backuplocal.terms.LocalBackupTermsScreen
import io.horizontalsystems.core.findNavController

class BackupLocalFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent() {
        val accountId = arguments?.getString(ACCOUNT_ID_KEY)
        if (accountId != null) {
            SingleWalletBackupNavHost(findNavController(), accountId)
        } else {
            FullBackupNavHost(fragmentNavController = findNavController())
        }
    }

    companion object {
        private const val ACCOUNT_ID_KEY = "coin_uid_key"
        fun prepareParams(accountId: String): Bundle {
            return bundleOf(ACCOUNT_ID_KEY to accountId)
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
                    val accountIds = accountIdsList.joinToString(separator = ",")
                    navController.navigate("terms_page/${accountIds}")
                },
                onBackClick = {
                    fragmentNavController.popBackStack()
                }
            )
        }

        composablePage("terms_page/{accountIds}") { backStackEntry ->
            val accountIds = backStackEntry.arguments?.getString("accountIds")
            LocalBackupTermsScreen(
                onTermsAccepted = {
                    navController.navigate("password_page/${accountIds}")
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composablePage("password_page/{accountIds}") { backStackEntry ->
            val accountIds = backStackEntry.arguments?.getString("accountIds")?.split(",") ?: listOf()
            Log.e("eee", "password_page selectedWallets ${accountIds.size}: ${backStackEntry.arguments?.getString("accountIds")}")

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
        startDestination = "terms_page",
    ) {
        composable("terms_page") {
            LocalBackupTermsScreen(
                onTermsAccepted = {
                    navController.navigate("password_page")
                },
                onBackClick = {
                    fragmentNavController.popBackStack()
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
                    fragmentNavController.popBackStack()
                }
            )
        }
    }
}
