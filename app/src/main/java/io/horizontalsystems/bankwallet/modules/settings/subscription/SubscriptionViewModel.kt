package io.horizontalsystems.bankwallet.modules.settings.subscription

import android.content.Context
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.UserSubscription
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class SubscriptionViewModel : ViewModelUiState<ManageSubscriptionUiState>() {
    private var userHasActiveSubscription = false

    init {
        viewModelScope.launch {
            UserSubscriptionManager.activeSubscriptionStateFlow.collect {
                refreshData(it)
                emitState()
            }
        }
    }

    override fun createState() = ManageSubscriptionUiState(
        userHasActiveSubscription = userHasActiveSubscription
    )

    private fun refreshData(userSubscription: UserSubscription?) {
        userHasActiveSubscription = userSubscription != null
    }

    fun restorePurchase() {
        viewModelScope.launch {
            UserSubscriptionManager.restore()
        }
    }

    fun launchManageSubscriptionScreen(context: Context) {
        UserSubscriptionManager.launchManageSubscriptionScreen(context)
    }
}

data class ManageSubscriptionUiState(
    val userHasActiveSubscription: Boolean,
)
