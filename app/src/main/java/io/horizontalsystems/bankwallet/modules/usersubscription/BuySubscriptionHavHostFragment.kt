package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.core.composablePopup
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PremiumSubscribedScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object BuySubscriptionHavHostScreen : HSScreen()

class BuySubscriptionHavHostFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
        SubscriptionNavHost(
            navController = navController,
            onClose = { navController.popBackStack() })
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable
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
            PremiumFeaturesScreen(
                navController,
                navHostController,
                onClose = onClose
            )
        }
        composablePopup("premium_subscribed_page") {
            PremiumSubscribedScreen(
                onCloseClick = onClose
            )
        }
    }
}