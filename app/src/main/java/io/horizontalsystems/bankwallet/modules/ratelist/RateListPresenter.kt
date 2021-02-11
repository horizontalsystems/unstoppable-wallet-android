package io.horizontalsystems.bankwallet.modules.ratelist

import androidx.lifecycle.ViewModel
import io.horizontalsystems.xrateskit.entities.MarketInfo

class RateListPresenter(
        val view: RateListView,
        val router: RateListRouter,
        private val interactor: RateListModule.IInteractor,
        private val factory: RateListModule.IRateListFactory
) : ViewModel(), RateListModule.IViewDelegate, RateListModule.IInteractorDelegate {

    private var portfolioMarketInfos = mutableMapOf<String, MarketInfo>()
    private val topMarketInfos = mutableListOf<TopMarketRanked>()

    private var loading = false

    private val coins = interactor.coins
    private val currency = interactor.currency
    private var inited = false
    private var sortType: TopListSortType = TopListSortType.Rank

    //IViewDelegate

    override fun viewDidLoad() {
        if (inited) return
        inited = true

        coins.map { coin ->
            interactor.getMarketInfo(coin.code, currency.code)?.let { marketInfo ->
                portfolioMarketInfos.put(coin.code, marketInfo)
            }
        }

        updateViewItems()

        lastUpdateTimestamp(portfolioMarketInfos)?.let {
            view.setDate(it)
        }

        interactor.setupXRateManager(coins)
        interactor.subscribeToMarketInfo(currency.code)
    }

    fun loadTopList() {
        if (!loading) {
            loading = true
            interactor.getTopList()
            updateViewItems()
        }
    }

    override fun onCoinClicked(coinItem: CoinItem) {
        router.openChart(coinItem.coinCode, coinItem.coinName)
    }

    override fun onTopListSortClick() {
        router.openSortingTypeDialog(sortType)
    }

    override fun onTopListSortTypeChange(sortType: TopListSortType) {
        this.sortType = sortType
        sortTopList()
        updateViewItems()
    }

    //IInteractorDelegate

    override fun didUpdateMarketInfo(marketInfos: Map<String, MarketInfo>) {
        portfolioMarketInfos.clear()
        portfolioMarketInfos.putAll(marketInfos)

        syncListsAndShow()
    }

    override fun didFetchedTopMarketList(items: List<TopMarketRanked>) {
        loading = false

        topMarketInfos.clear()
        topMarketInfos.addAll(items)

        sortTopList()

        syncListsAndShow()
    }

    private fun sortTopList() {
        when (sortType) {
            TopListSortType.Rank -> topMarketInfos.sortBy { it.rank }
            TopListSortType.Winners -> topMarketInfos.sortByDescending { it.marketInfo.rateDiff }
            TopListSortType.Losers -> topMarketInfos.sortByDescending { -it.marketInfo.rateDiff }
        }
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
                        topMarketInfos[index] = TopMarketRanked(topMarket.coinCode, topMarket.coinName, portfolioMarketInfo, topMarket.rank)
                    } else {
                        portfolioMarketInfos[coinCode] = topMarket.marketInfo
                    }
                }
            }
        }

        lastUpdateTimestamp(portfolioMarketInfos)?.let {
            view.setDate(it)
        }

        updateViewItems()
    }

    private fun updateViewItems() {
        view.setTopViewItems(factory.topListViewItems(topMarketInfos, currency))
        view.setPortfolioViewItems(factory.portfolioViewItems(coins, currency, portfolioMarketInfos))
    }

    private fun lastUpdateTimestamp(marketInfos: Map<String, MarketInfo?>): Long? {
        val allTimestamps = marketInfos.map { it.value?.timestamp }.filterNotNull()
        return allTimestamps.maxOrNull()
    }

}
