package com.quantum.wallet.bankwallet.modules.settings.subscription

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.quantum.wallet.bankwallet.core.ViewModelUiState
import com.quantum.wallet.subscriptions.core.UserSubscription
import com.quantum.wallet.subscriptions.core.UserSubscriptionManager
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
