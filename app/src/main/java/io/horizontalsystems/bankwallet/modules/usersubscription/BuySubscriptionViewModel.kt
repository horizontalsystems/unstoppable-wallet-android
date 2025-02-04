package io.horizontalsystems.bankwallet.modules.usersubscription

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscriptions = listOf<Subscription>()
    private var selectedTabIndex = 0
    private var hasFreeTrial = false

    init {
        viewModelScope.launch {
            subscriptions = UserSubscriptionManager.getSubscriptions()
            refreshHasFreeTrial()

            emitState()
        }
    }

    override fun createState() = BuySubscriptionUiState(
        subscriptions = subscriptions,
        selectedTabIndex = selectedTabIndex,
        hasFreeTrial = hasFreeTrial,
    )

    fun setSelectedTabIndex(index: Int) {
        selectedTabIndex = index
        refreshHasFreeTrial()

        emitState()
    }

    private fun refreshHasFreeTrial() {
        val subscription = subscriptions[selectedTabIndex]
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
    val subscriptions: List<Subscription>,
    val selectedTabIndex: Int,
    val hasFreeTrial: Boolean
)
