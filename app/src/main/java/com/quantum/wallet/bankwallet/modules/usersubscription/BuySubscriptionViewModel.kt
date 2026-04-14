package com.quantum.wallet.bankwallet.modules.usersubscription

import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.subscriptions.core.AdvancedSearch
import com.quantum.wallet.subscriptions.core.IPaidAction
import com.quantum.wallet.subscriptions.core.PrioritySupport
import com.quantum.wallet.subscriptions.core.RobberyProtection
import com.quantum.wallet.subscriptions.core.ScamProtection
import com.quantum.wallet.subscriptions.core.SecureSend
import com.quantum.wallet.subscriptions.core.Subscription
import com.quantum.wallet.subscriptions.core.SwapProtection
import com.quantum.wallet.subscriptions.core.TokenInsights
import com.quantum.wallet.subscriptions.core.TradeSignals
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscription: Subscription? = null
    private var hasFreeTrial = false
    private val defenseSystemFeatures = listOf(
        SecureSend, ScamProtection, SwapProtection, RobberyProtection,
    )

    private val advancedControlsFeatures = listOf(PrioritySupport)

    private val marketInsightsFeatures = listOf(
        TokenInsights, AdvancedSearch, TradeSignals
    )


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
    val defenseSystemFeatures: List<IPaidAction>,
    val marketInsightsFeatures: List<IPaidAction>,
    val advancedControlsFeatures: List<IPaidAction>,
    val hasFreeTrial: Boolean
)
