package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.PriceInfo
import java.util.*

class RateListPresenter(
        val view: RateListView,
        private val interactor: RateListModule.IInteractor,
        private val factory: RateListModule.IRateListFactory
) : ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate {

    //IViewDelegate

    override fun viewDidLoad() {
        val coins = interactor.coins
        val currency = interactor.currency

        val marketInfos = coins.map { it.code to interactor.getMarketInfo(it.code, currency.code)}.toMap()

        view.setDates(Date(), lastUpdateTimestamp(marketInfos))

        val items = factory.portfolioViewItems(coins, currency, marketInfos)
        view.showPortfolioItems(items)

        val coinCodes = coins.map { it.code }
        interactor.setupXRateManager(coinCodes)
        interactor.subscribeToMarketInfo(currency.code)
    }

    override fun loadTopList(shownSize: Int) {
        if (shownSize == 0) {
            interactor.getTopList()
        }
    }

    //IInteractorDelegate

    override fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>) {
        val items = factory.portfolioViewItems(interactor.coins, interactor.currency, marketInfos)

        view.setDates(Date(), lastUpdateTimestamp(marketInfos))

        view.showPortfolioItems(items)
    }

    override fun didFetchedTopList(items: List<PriceInfo>) {
        val viewItems =factory.topListViewItems(items, interactor.currency)
        view.showTopListItems(viewItems)
    }

    override fun onCleared() {
        super.onCleared()
        interactor.clear()
    }

    private fun lastUpdateTimestamp(marketInfos: Map<String, MarketInfo?>): Long? {
        val allTimestamps = marketInfos.map { it.value?.timestamp }.filterNotNull()
        return allTimestamps.max()
    }

}
