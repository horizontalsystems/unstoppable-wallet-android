package io.horizontalsystems.bankwallet.modules.premium

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize

class PremiumFragment : BaseComposeFragment() {

    @Composable
    override fun GetContent(navController: NavController) {
        ComposeAppTheme {
            PremiumNavHost(
                navController = navController,
                onClose = { navController.popBackStack() }
            )
        }
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

    @Parcelize
    class Result : Parcelable
}

@Composable
fun PremiumNavHost(
    navController: NavController,
    onClose: () -> Unit
) {
    val navHostController = rememberNavController()
    NavHost(
        navController = navHostController,
        startDestination = "select_premium_plan",
    ) {
        composable("select_premium_plan") {
            SelectPremiumPlanScreen(
                navHostController,
                onCloseClick = onClose
            )
        }
        composablePage(
            "premium_subscribed_page?type={type}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
            )
        ) { navBackStackEntry ->
            val type =
                navBackStackEntry.arguments?.getString("type") ?: PremiumPlanType.ProPlan.name
            PremiumSubscribedScreen(
                type = PremiumPlanType.valueOf(type),
                onCloseClick = onClose
            )
        }
    }
}