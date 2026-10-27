package io.horizontalsystems.walletkit.modules.usersubscription

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.walletkit.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.TradeSignals
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscription: Subscription? = null
    private var hasFreeTrial = false
    private val advancedControlsFeatures = listOf(PrioritySupport)

    private val marketInsightsFeatures = listOf(
        AdvancedSearch, TradeSignals
    )


    init {
        viewModelScope.launch {
            subscription = UserSubscriptionManager.getSubscriptions().firstOrNull()
            refreshHasFreeTrial()

            emitState()
        }
    }

    override fun createState() = BuySubscriptionUiState(
        marketInsightsFeatures = marketInsightsFeatures,
        advancedControlsFeatures = advancedControlsFeatures,
        hasFreeTrial = hasFreeTrial,
    )

    private fun refreshHasFreeTrial() {
        val subscription = subscription ?: return
        val basePlans = UserSubscriptionManager.getBasePlans(subscription.id)
        hasFreeTrial = basePlans.any { it.hasFreeTrial }
    }

    fun restore() {
        viewModelScope.launch {
            UserSubscriptionManager.restore()
        }
    }
}

data class BuySubscriptionUiState(
    val marketInsightsFeatures: List<IPaidAction>,
    val advancedControlsFeatures: List<IPaidAction>,
    val hasFreeTrial: Boolean
)
