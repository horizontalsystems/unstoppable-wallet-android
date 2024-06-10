package io.horizontalsystems.bankwallet.modules.usersubscription

import android.app.Activity
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptionskit.SubscriptionPlan
import io.horizontalsystems.subscriptionskit.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var plans = listOf<SubscriptionPlan>()

    override fun createState() = BuySubscriptionUiState(
        plans = plans
    )

    fun launchPurchaseFlow(planId: String, activity: Activity) {
        UserSubscriptionManager.launchPurchaseFlow(planId, activity)
    }

    init {
        viewModelScope.launch {
            plans = UserSubscriptionManager.getPlans()

            emitState()
        }
    }
}

data class BuySubscriptionUiState(val plans: List<SubscriptionPlan>)
