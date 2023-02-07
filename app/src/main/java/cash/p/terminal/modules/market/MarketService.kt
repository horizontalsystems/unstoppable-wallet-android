package cash.p.terminal.modules.market

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.IMarketStorage
import cash.p.terminal.entities.LaunchPage

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
