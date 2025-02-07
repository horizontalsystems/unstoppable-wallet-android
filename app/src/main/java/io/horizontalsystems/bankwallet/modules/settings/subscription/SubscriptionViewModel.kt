package io.horizontalsystems.bankwallet.modules.settings.subscription

import android.content.Context
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class SubscriptionViewModel : ViewModelUiState<ManageSubscriptionUiState>() {
    private var userHasActiveSubscription = false

    init {
        viewModelScope.launch {
            UserSubscriptionManager.purchaseStateUpdatedFlow.collect {
                refreshData()
                emitState()
            }
        }

        refreshData()
        emitState()
    }

    override fun createState() = ManageSubscriptionUiState(
        userHasActiveSubscription = userHasActiveSubscription
    )

    private fun refreshData() {
        userHasActiveSubscription = UserSubscriptionManager.getActiveUserSubscriptions().isNotEmpty()
    }

    fun restorePurchase() {
        TODO("Not yet implemented")
    }

    fun launchManageSubscriptionScreen(context: Context) {
        UserSubscriptionManager.launchManageSubscriptionScreen(context)
    }
}

data class ManageSubscriptionUiState(
    val userHasActiveSubscription: Boolean,
)
