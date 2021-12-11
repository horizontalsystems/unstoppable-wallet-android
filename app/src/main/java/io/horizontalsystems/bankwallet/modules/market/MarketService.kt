package io.horizontalsystems.bankwallet.modules.market

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IMarketStorage
import io.horizontalsystems.bankwallet.entities.LaunchPage

class MarketService(
    private val storage: IMarketStorage,
    private val localStorage: ILocalStorage,
) {
    val launchPage: LaunchPage?
        get() = localStorage.launchPage

    var currentTab: MarketModule.Tab?
        get() = storage.currentMarketTab
        set(value) {
            storage.currentMarketTab = value
        }

}
