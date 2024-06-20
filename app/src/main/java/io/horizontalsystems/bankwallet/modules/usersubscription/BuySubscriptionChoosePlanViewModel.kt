package io.horizontalsystems.bankwallet.modules.usersubscription

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionChoosePlanViewModel(private val subscriptionId: String) : ViewModelUiState<BuySubscriptionChoosePlanUiState>() {
    private var basePlans: List<BasePlan> = listOf()
    private var purchaseInProgress = false
    private var purchase: HSPurchase? = null
    private var error: Throwable? = null

    init {
        viewModelScope.launch {
            basePlans = UserSubscriptionManager.getBasePlans(subscriptionId)

            emitState()
        }
    }

    override fun createState() = BuySubscriptionChoosePlanUiState(
        basePlans = basePlans,
        purchaseInProgress = purchaseInProgress,
        error = error,
        purchase = purchase
    )

    fun launchPurchaseFlow(planId: String, activity: Activity) {
        purchaseInProgress = true
        error = null
        emitState()

        viewModelScope.launch {
            try {
                val hsPurchase =
                    UserSubscriptionManager.launchPurchaseFlow(subscriptionId, planId, activity)

                purchase = hsPurchase
            } catch (e: Throwable) {
                error = e
            }

            purchaseInProgress = false
            emitState()
        }
    }

    class Factory(private val subscriptionId: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BuySubscriptionChoosePlanViewModel(subscriptionId) as T
        }
    }
}

data class BuySubscriptionChoosePlanUiState(
    val basePlans: List<BasePlan>,
    val purchaseInProgress: Boolean,
    val error: Throwable?,
    val purchase: HSPurchase?
) {
    val choosePlanEnabled = !purchaseInProgress
}
