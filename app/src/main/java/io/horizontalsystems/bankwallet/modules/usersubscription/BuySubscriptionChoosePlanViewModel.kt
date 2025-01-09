package io.horizontalsystems.bankwallet.modules.usersubscription

import android.app.Activity
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionChoosePlanViewModel : ViewModelUiState<BuySubscriptionChoosePlanUiState>() {
    private var basePlans: List<BasePlan> = listOf()
    private var purchaseInProgress = false
    private var purchase: HSPurchase? = null
    private var error: Throwable? = null

    override fun createState() = BuySubscriptionChoosePlanUiState(
        basePlans = basePlans,
        purchaseInProgress = purchaseInProgress,
        error = error,
        purchase = purchase
    )

    fun getBasePlans(subscriptionId: String) {
        viewModelScope.launch {
            try {
                basePlans = UserSubscriptionManager.getBasePlans(subscriptionId)
                emitState()
            } catch (e: Throwable) {
                error = e
                emitState()
            }
        }
    }

    fun launchPurchaseFlow(subscriptionId: String, planId: String, activity: Activity) {
        purchaseInProgress = true
        error = null
        emitState()

        viewModelScope.launch {
            try {
                val hsPurchase =
                    UserSubscriptionManager.launchPurchaseFlow(subscriptionId, planId, activity)

                purchase = hsPurchase
                if (hsPurchase == null) {
                    error = Throwable("Purchase failed")
                }
            } catch (e: Throwable) {
                error = e
            }
            UserSubscriptionManager.purchaseStateUpdated()
            purchaseInProgress = false
            emitState()
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
