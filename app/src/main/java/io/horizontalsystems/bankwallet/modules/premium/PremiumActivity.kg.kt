package io.horizontalsystems.bankwallet.modules.premium

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.horizontalsystems.bankwallet.core.BaseActivity
import io.horizontalsystems.bankwallet.core.composablePage
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

class PremiumActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ComposeAppTheme {
                PremiumNavHost(
                    onClose = { finish() }
                )
            }
        }
        setStatusBarTransparent()
    }

}

@Composable
fun PremiumNavHost(
    onClose: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = "select_premium_plan",
    ) {
        composable("select_premium_plan") {
            SelectPremiumPlanScreen(
                navController,
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