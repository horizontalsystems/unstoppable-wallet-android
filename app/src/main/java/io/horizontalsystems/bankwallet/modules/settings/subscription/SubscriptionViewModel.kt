package io.horizontalsystems.bankwallet.modules.settings.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class SubscriptionViewModel : ViewModelUiState<SubscriptionViewModel.ViewItem>() {

    private var subscriptionName: String? =
        UserSubscriptionManager.getActiveSubscriptions().firstOrNull()?.name

    init {
        viewModelScope.launch {
            UserSubscriptionManager.purchaseStateUpdatedFlow.collect {
                subscriptionName =
                    UserSubscriptionManager.getActiveSubscriptions().firstOrNull()?.name
                emitState()
            }
        }
    }

    override fun createState(): ViewItem {
        return ViewItem(
            subscriptionName = subscriptionName
        )
    }

    data class ViewItem(
        val subscriptionName: String?,
    )
}


object SubscriptionModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SubscriptionViewModel() as T
        }
    }
}