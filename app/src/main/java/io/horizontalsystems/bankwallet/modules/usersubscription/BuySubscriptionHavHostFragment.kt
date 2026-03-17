package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation3.runtime.NavBackStack
import io.horizontalsystems.bankwallet.core.BaseComposeFragment
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object BuySubscriptionHavHostScreen : HSScreen() {
    @Composable
    override fun GetContent(
        backStack: NavBackStack<HSScreen>
    ) {
        PremiumFeaturesScreen(
            backStack = backStack,
            onClose = backStack::removeLastOrNull
        )
    }
}

class BuySubscriptionHavHostFragment : BaseComposeFragment() {
    @Composable
    override fun GetContent(navController: NavController) {
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable
}
