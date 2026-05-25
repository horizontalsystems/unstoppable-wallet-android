package io.horizontalsystems.bankwallet.modules.usersubscription

import android.os.Parcelable
import androidx.compose.runtime.Composable
import io.horizontalsystems.bankwallet.modules.nav3.HSNavigation
import io.horizontalsystems.bankwallet.modules.nav3.HSPage
import io.horizontalsystems.bankwallet.modules.nav3.LocalResultEventBus
import io.horizontalsystems.bankwallet.modules.usersubscription.ui.PremiumSubscribedScreen
import io.horizontalsystems.subscriptions.core.IPaidAction
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
data object BuySubscriptionHavHostPage : HSPage() {
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
data object PremiumSubscribedPage : HSPage() {
    @Composable
    override fun GetContent(navController: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        PremiumSubscribedScreen(
            onCloseClick = {
                resultEventBus.sendResult(Result())
                navController.removeLastUntil(BuySubscriptionHavHostPage::class, true)
            }
        )
    }

    @Parcelize
    class Result : Parcelable
}
