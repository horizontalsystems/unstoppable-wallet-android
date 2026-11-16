package io.horizontalsystems.walletkit.modules.usersubscription

import androidx.compose.runtime.Composable
import io.horizontalsystems.walletkit.modules.nav3.HSNavigation
import io.horizontalsystems.walletkit.modules.nav3.HSPage
import io.horizontalsystems.walletkit.modules.nav3.LocalResultEventBus
import io.horizontalsystems.walletkit.modules.usersubscription.ui.PremiumSubscribedScreen
import kotlinx.serialization.Serializable

@Serializable
data object BuySubscriptionHavHostPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        PremiumFeaturesScreen(
            navigation,
            onClose = { navigation.removeLastOrNull() }
        )
    }
}

@Serializable
data object PremiumSubscribedPage : HSPage() {
    @Composable
    override fun GetContent(navigation: HSNavigation) {
        val resultEventBus = LocalResultEventBus.current
        PremiumSubscribedScreen(
            onCloseClick = {
                resultEventBus.sendResult(Result())
                navigation.removeLastUntil(BuySubscriptionHavHostPage::class, true)
            }
        )
    }

    class Result
}
