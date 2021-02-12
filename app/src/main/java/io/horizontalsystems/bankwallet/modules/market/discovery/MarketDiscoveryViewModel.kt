package io.horizontalsystems.bankwallet.modules.market.discovery

import androidx.lifecycle.ViewModel

class MarketDiscoveryViewModel(private val service: MarketDiscoveryService) : ViewModel() {

    val marketCategories by service::marketCategories
    var marketCategory by service::marketCategory

}
