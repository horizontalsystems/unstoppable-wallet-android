package io.horizontalsystems.bankwallet.modules.importcexaccount

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.core.getInput
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.modules.info.ErrorDisplayDialogFragment
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.core.helpers.HudHelper

class ImportCexAccountFragment : BaseComposeFragment(screenshotEnabled = false) {

    @Composable
    override fun GetContent(navController: NavController) {
        val input = navController.getInput<ManageAccountsModule.Input>()
        val popUpToInclusiveId = input?.popOffOnSuccess ?: R.id.restoreAccountFragment
        val inclusive = input?.popOffInclusive ?: false

        ImportCexAccountNavHost(navController, popUpToInclusiveId, inclusive)
    }

}


@Composable
fun ImportCexAccountNavHost(
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "choose-cex",
    ) {
        composable("choose-cex") {
            ImportCexAccountSelectCexScreen(
                onSelectCex = { navController.navigate("enter-cex-data/$it") },
                onNavigateBack = { fragmentNavController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() }
            )
        }
        composablePage("enter-cex-data/{cexId}") { backStackEntry ->
            val view = LocalView.current
            ImportCexAccountEnterCexDataScreen(
                cexId = backStackEntry.arguments?.getString("cexId") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
                onAccountCreate = {
                    HudHelper.showSuccessMessage(
                        contenView = view,
                        resId = R.string.Hud_Text_Connected,
                        icon = R.drawable.icon_link_24,
                        iconTint = R.color.white
                    )
                    fragmentNavController.popBackStack(popUpToInclusiveId, inclusive)
                },
                onShowError = { title, text ->
                    fragmentNavController.slideFromBottom(
                        R.id.errorDisplayDialogFragment,
                        ErrorDisplayDialogFragment.Input(title.toString(), text.toString())
                    )
                }
            )
        }
    }
}
