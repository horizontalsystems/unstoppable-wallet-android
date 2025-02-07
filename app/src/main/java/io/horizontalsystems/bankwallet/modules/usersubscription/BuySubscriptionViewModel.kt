package io.horizontalsystems.bankwallet.modules.usersubscription

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscription: Subscription? = null
    private var hasFreeTrial = false

    init {
        viewModelScope.launch {
            subscription = UserSubscriptionManager.getSubscriptions().firstOrNull()
            refreshHasFreeTrial()

            emitState()
        }
    }

    override fun createState() = BuySubscriptionUiState(
        subscription = subscription,
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
    val subscription: Subscription?,
    val hasFreeTrial: Boolean
)
