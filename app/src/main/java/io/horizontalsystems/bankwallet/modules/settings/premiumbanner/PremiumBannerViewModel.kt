package io.horizontalsystems.bankwallet.modules.settings.premiumbanner

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.subscriptions.core.UserSubscriptionManager
import kotlinx.coroutines.launch

class PremiumBannerViewModel: ViewModelUiState<PremiumBannerUiState>() {
    private var hasFreeTrial = false

    init {
        viewModelScope.launch {
            val subscriptions = UserSubscriptionManager.getSubscriptions()
            val plans = subscriptions.flatMap { subscription ->
                UserSubscriptionManager.getBasePlans(subscription.id)
            }
            hasFreeTrial = plans.any {  it.hasFreeTrial }

            emitState()
        }
    }

    override fun createState() = PremiumBannerUiState(
        hasFreeTrial = hasFreeTrial
    )

}

data class PremiumBannerUiState(
    val hasFreeTrial: Boolean
)