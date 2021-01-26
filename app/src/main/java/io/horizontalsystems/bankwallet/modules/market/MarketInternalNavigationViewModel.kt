package io.horizontalsystems.bankwallet.modules.market

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.modules.market.discovery.MarketDiscoveryFragment
import io.horizontalsystems.core.SingleLiveEvent

class MarketInternalNavigationViewModel : ViewModel() {

    val navigateToDiscoveryLiveEvent = SingleLiveEvent<MarketDiscoveryFragment.Mode>()
    val discoveryModeLiveEvent = SingleLiveEvent<MarketDiscoveryFragment.Mode>()

    fun navigateToDiscovery(mode: MarketDiscoveryFragment.Mode) {
        navigateToDiscoveryLiveEvent.value = mode
    }

    fun setDiscoveryMode(mode: MarketDiscoveryFragment.Mode) {
        discoveryModeLiveEvent.value = mode
    }
}
