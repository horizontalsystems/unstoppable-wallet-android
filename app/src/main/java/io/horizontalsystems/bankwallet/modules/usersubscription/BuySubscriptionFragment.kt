package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.core.setNavigationResultX
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PremiumPlanType
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PremiumSubscribedScreen
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.SelectSubscriptionScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize

class BuySubscriptionFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SubscriptionNavHost(
            navController = navController,
            onClose = { navController.popBackStack() })
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

    @Parcelize
    class Result : Parcelable
}

@Composable
fun SubscriptionNavHost(
    navController: NavController,
    onClose: () -> Unit
) {
    val navHostController = rememberNavController()
    NavHost(
        navController = navHostController,
        startDestination = "select_subscription",
    ) {
        composable("select_subscription") {
            SelectSubscriptionScreen(
                navHostController,
                onCloseClick = onClose
            )
        }
        composablePopup(
            "premium_subscribed_page?type={type}",
            arguments = listOf(
                navArgument("type") {
                    type = NavType.StringType
                    defaultValue = null
                    nullable = true
                },
            )
        ) { navBackStackEntry ->
            val type = navBackStackEntry.arguments?.getString("type")
                ?: PremiumPlanType.ProPlan.name
            PremiumSubscribedScreen(
                type = PremiumPlanType.valueOf(type),
                onCloseClick = {
                    navController.setNavigationResultX(BuySubscriptionFragment.Result())
                    onClose()
                }
            )
        }
    }
}
