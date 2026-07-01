package io.horizontalsystems.bankwallet.modules.usersubscription

import android.app.Activity
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.stats.StatEvent
import io.horizontalsystems.bankwallet.core.stats.StatPage
import io.horizontalsystems.bankwallet.core.stats.stat
import io.horizontalsystems.subscriptions.core.BasePlan
import io.horizontalsystems.subscriptions.core.HSPurchase
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch
import java.time.Period

class BuySubscriptionChoosePlanViewModel : ViewModelUiState<BuySubscriptionChoosePlanUiState>() {
    private var freeTrialPeriod: Period? = null
    private var basePlans: List<BasePlan> = listOf()
    private var purchaseInProgress = false
    private var purchase: HSPurchase? = null
    private var error: Throwable? = null
    private var selectedIndex = 0
    private var subscriptionId: String? = null

    override fun createState() = BuySubscriptionChoosePlanUiState(
        basePlans = basePlans,
        purchaseInProgress = purchaseInProgress,
        error = error,
        purchase = purchase,
        selectedIndex = selectedIndex,
        freeTrialPeriod = freeTrialPeriod,
        subscriptionId = subscriptionId
    )

    fun getBasePlans() {
        viewModelScope.launch {
            val subscription = UserSubscriptionManager.getSubscriptions().firstOrNull()
            subscriptionId = subscription?.id

            try {
                subscriptionId?.let {
                    basePlans = UserSubscriptionManager.getBasePlans(it)
                        .sortedByDescending { it.pricingPhases.last().numberOfDays }

                    refreshFreeTrialPeriod()

                    emitState()
                }
            } catch (e: Throwable) {
                error = e
                emitState()
            }
        }
    }

    fun launchPurchaseFlow(subscriptionId: String, offerToken: String, activity: Activity) {
        purchaseInProgress = true
        error = null
        emitState()

        viewModelScope.launch {
            try {
                val hsPurchase =
                    UserSubscriptionManager.launchPurchaseFlow(subscriptionId, offerToken, activity)

                purchase = hsPurchase

                stat(page = StatPage.PurchaseSelector, event = StatEvent.Subscribe)
            } catch (e: Throwable) {
                error = e
            }
            purchaseInProgress = false
            emitState()
        }
    }

    fun select(index: Int) {
        selectedIndex = index

        refreshFreeTrialPeriod()

        emitState()
    }

    private fun refreshFreeTrialPeriod() {
        freeTrialPeriod = basePlans[selectedIndex].feePricingPhase?.period
    }

    fun onErrorHandled() {
        error = null

        emitState()
    }
}

data class BuySubscriptionChoosePlanUiState(
    val basePlans: List<BasePlan>,
    val purchaseInProgress: Boolean,
    val error: Throwable?,
    val purchase: HSPurchase?,
    val selectedIndex: Int,
    val freeTrialPeriod: Period?,
    val subscriptionId: String?,
) {
    val choosePlanEnabled = !purchaseInProgress
}
