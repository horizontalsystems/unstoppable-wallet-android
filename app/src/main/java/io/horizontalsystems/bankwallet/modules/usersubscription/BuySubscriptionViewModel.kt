package io.horizontalsystems.bankwallet.modules.usersubscription

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.Subscription
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class BuySubscriptionViewModel : ViewModelUiState<BuySubscriptionUiState>() {
    private var subscriptions = listOf<Subscription>()

    init {
        viewModelScope.launch {
            subscriptions = UserSubscriptionManager.getSubscriptions()

            emitState()
        }
    }

    override fun createState() = BuySubscriptionUiState(
        subscriptions = subscriptions
    )
}

data class BuySubscriptionUiState(
    val subscriptions: List<Subscription>
)
