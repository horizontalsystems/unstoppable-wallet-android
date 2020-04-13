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

    private var portfolioItems = mutableListOf<ViewItem.CoinViewItem>()
    private var topListItems = mutableListOf<ViewItem.CoinViewItem>()
    private var loading = false

    //IViewDelegate

    override fun viewDidLoad() {
        val coins = interactor.coins
        val currency = interactor.currency

        val marketInfos = coins.map { it.code to interactor.getMarketInfo(it.code, currency.code)}.toMap()

        portfolioItems.addAll(factory.portfolioViewItems(coins, currency, marketInfos))

        updateViewItems()

        view.setDates(Date(), lastUpdateTimestamp(marketInfos))

        val coinCodes = coins.map { it.code }
        interactor.setupXRateManager(coinCodes)
        interactor.subscribeToMarketInfo(currency.code)
    }

    override fun loadTopList() {
        if (topListItems.isEmpty() && !loading) {
            loading = true
            interactor.getTopList()
            updateViewItems()
        }
    }

    //IInteractorDelegate

    override fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>) {
        val items = factory.portfolioViewItems(interactor.coins, interactor.currency, marketInfos)
        portfolioItems.clear()
        portfolioItems.addAll(items)
        updateViewItems()

        view.setDates(Date(), lastUpdateTimestamp(marketInfos))
    }

    override fun didFetchedTopList(items: List<PriceInfo>) {
        loading = false
        val viewItems =factory.topListViewItems(items, interactor.currency)
        topListItems.addAll(viewItems)

        updateViewItems()
    }

    override fun didFailToFetchTopList() {
        loading = false
        updateViewItems()
    }

    override fun onCleared() {
        super.onCleared()
        interactor.clear()
    }

    private fun updateViewItems() {
        val viewItems: List<ViewItem> = factory.getViewItems(portfolioItems, topListItems, loading)
        view.setViewItems(viewItems)
    }

    private fun lastUpdateTimestamp(marketInfos: Map<String, MarketInfo?>): Long? {
        val allTimestamps = marketInfos.map { it.value?.timestamp }.filterNotNull()
        return allTimestamps.max()
    }

}
