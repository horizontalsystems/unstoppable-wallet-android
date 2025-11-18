package io.horizontalsystems.bankwallet.modules.usersubscription

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.AdvancedSearch
import io.horizontalsystems.subscriptions.core.IPaidAction
import io.horizontalsystems.subscriptions.core.LossProtection
import io.horizontalsystems.subscriptions.core.PrioritySupport
import io.horizontalsystems.subscriptions.core.RobberyProtection
import io.horizontalsystems.subscriptions.core.ScamProtection
import io.horizontalsystems.subscriptions.core.SecureSend
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.TokenInsights
import io.horizontalsystems.subscriptions.core.TradeSignals
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscription: Subscription? = null
    private var hasFreeTrial = false
    private val defenseSystemFeatures = listOf(
        SecureSend, ScamProtection, LossProtection, RobberyProtection,
    )
    private val marketInsightsFeatures = listOf(
        TokenInsights, AdvancedSearch, TradeSignals
    )

    private val vipFeatures = listOf(PrioritySupport)


    init {
        viewModelScope.launch {
            subscription = UserSubscriptionManager.getSubscriptions().firstOrNull()
            refreshHasFreeTrial()

            emitState()
        }
    }

    override fun createState() = BuySubscriptionUiState(
        defenseSystemFeatures = defenseSystemFeatures,
        marketInsightsFeatures = marketInsightsFeatures,
        vipFeatures = vipFeatures,
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
    val defenseSystemFeatures: List<IPaidAction>,
    val marketInsightsFeatures: List<IPaidAction>,
    val vipFeatures: List<IPaidAction>,
    val hasFreeTrial: Boolean
)
