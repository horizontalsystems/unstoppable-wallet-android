package io.horizontalsystems.bankwallet.modules.importcexaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.NavController
import androidx.navigation.findNavController
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.modules.manageaccounts.ManageAccountsModule
import io.horizontalsystems.bankwallet.modules.settings.about.*
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.*

class ImportCexAccountFragment: BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )
            val popUpToInclusiveId =
                arguments?.getInt(ManageAccountsModule.popOffOnSuccessKey, R.id.restoreAccountFragment) ?: R.id.restoreAccountFragment

            val inclusive =
                arguments?.getBoolean(ManageAccountsModule.popOffInclusiveKey) ?: false

            setContent {
                ComposeAppTheme {
                    ImportCexAccountNavHost(findNavController(), popUpToInclusiveId, inclusive)
                }
            }
        }
    }

}


@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ImportCexAccountNavHost(
    fragmentNavController: NavController,
    popUpToInclusiveId: Int,
    inclusive: Boolean
) {
    val navController = rememberAnimatedNavController()
    AnimatedNavHost(
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
            ImportCexAccountEnterCexDataScreen(
                cexId = backStackEntry.arguments?.getString("cexId") ?: "",
                onNavigateBack = { navController.popBackStack() },
                onClose = { fragmentNavController.popBackStack() },
                onAccountCreate = {
                    fragmentNavController.popBackStack(popUpToInclusiveId, inclusive)
                }
            )
        }
    }
}
