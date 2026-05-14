package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSScreen
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PremiumSubscribedScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object BuySubscriptionHavHostFragment : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        PremiumFeaturesScreen(
            navController,
            true,
            onClose = { navController.removeLastOrNull() }
        )
    }

    @Parcelize
    data class Input(val action: IPaidAction) : Parcelable

}

@Serializable
data object PremiumSubscribedPage : HSScreen() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        PremiumSubscribedScreen(
            onCloseClick = {
                resultEventBus.sendResult(Result())
                navController.removeLastUntil(BuySubscriptionHavHostFragment::class, true)
            }
        )
    }

    @Parcelize
    class Result : Parcelable
}
