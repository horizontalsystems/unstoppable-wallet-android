package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.horizontalsystems.xrateskit.entities.TopMarket

class RateListPresenter(
        val view: RateListView,
        val router: RateListRouter,
        private val interactor: RateListModule.IInteractor,
        private val factory: RateListModule.IRateListFactory
) : ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate {

    private var portfolioViewItems = mutableListOf<ViewItem.CoinViewItem>()
    private var topViewItems = mutableListOf<ViewItem.CoinViewItem>()

    private var portfolioMarketInfos = mutableMapOf<String, MarketInfo>()
    private var topMarketInfos = mutableListOf<TopMarket>()

    private var loading = false

    private val coins = interactor.coins
    private val currency = interactor.currency

    //IViewDelegate

    override fun viewDidLoad() {
        coins.map { coin ->
            interactor.getMarketInfo(coin.code, currency.code)?.let { marketInfo ->
                portfolioMarketInfos.put(coin.code, marketInfo)
            }
        }

        portfolioViewItems.addAll(factory.portfolioViewItems(coins, currency, portfolioMarketInfos))

        updateViewItems()

        lastUpdateTimestamp(portfolioMarketInfos)?.let {
            view.setDate(it)
        }

        val coinCodes = coins.map { it.code }
        interactor.setupXRateManager(coinCodes)
        interactor.subscribeToMarketInfo(currency.code)
        loadTopList()
    }

    private fun loadTopList() {
        if (!loading) {
            loading = true
            interactor.getTopList()
            updateViewItems()
        }
    }

    override fun onCoinClicked(coinViewItem: ViewItem.CoinViewItem) {
        router.openChart(coinViewItem.coinItem.coinCode, coinViewItem.coinItem.coinName)
    }

    //IInteractorDelegate

    override fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>) {
        portfolioMarketInfos.clear()
        portfolioMarketInfos.putAll(marketInfos)

        syncListsAndShow()
        loadTopList()
    }

    override fun didFetchedTopMarketList(items: List<TopMarket>) {
        loading = false

        topMarketInfos.clear()
        topMarketInfos.addAll(items)

        syncListsAndShow()
    }

    override fun didFailToFetchTopList() {
        loading = false
        updateViewItems()
    }

    override fun onCleared() {
        super.onCleared()
        interactor.clear()
    }

    private fun syncListsAndShow() {
        portfolioMarketInfos.forEach { (coinCode, portfolioMarketInfo) ->
            topMarketInfos.forEachIndexed { index, topMarket ->
                if (topMarket.coinCode == coinCode) {
                    if (portfolioMarketInfo.timestamp > topMarket.marketInfo.timestamp) {
                        topMarketInfos[index] = TopMarket(topMarket.coinCode, topMarket.coinName, portfolioMarketInfo)
                    } else {
                        portfolioMarketInfos[coinCode] = topMarket.marketInfo
                    }
                }
            }
        }

        topViewItems.clear()
        topViewItems.addAll(factory.topListViewItems(topMarketInfos, currency))
        portfolioViewItems.clear()
        portfolioViewItems.addAll(factory.portfolioViewItems(coins, currency, portfolioMarketInfos))

        lastUpdateTimestamp(portfolioMarketInfos)?.let {
            view.setDate(it)
        }

        updateViewItems()
    }

    private fun updateViewItems() {
        val viewItems: List<ViewItem> = factory.getViewItems(portfolioViewItems, topViewItems, loading)
        view.setViewItems(viewItems)
    }

    private fun lastUpdateTimestamp(marketInfos: Map<String, MarketInfo?>): Long? {
        val allTimestamps = marketInfos.map { it.value?.timestamp }.filterNotNull()
        return allTimestamps.max()
    }

}
