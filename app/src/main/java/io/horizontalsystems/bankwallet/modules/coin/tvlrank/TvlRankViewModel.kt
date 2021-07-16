package io.horizontalsystems.bankwallet.modules.coin.tvlrank

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.market.sortedByDescendingNullLast
import io.horizontalsystems.bankwallet.modules.market.sortedByNullLast
import io.horizontalsystems.xrateskit.entities.DefiTvl
import io.reactivex.disposables.Disposable

class TvlRankViewModel(
    private val numberFormatter: IAppNumberFormatter,
    private val xRateManager: IRateManager,
    appConfigProvider: IAppConfigProvider
) : ViewModel() {

    val coinList = MutableLiveData<List<TvlRankViewItem>>()
    val loadingLiveData = MutableLiveData(true)
    val coinInfoErrorLiveData = MutableLiveData<String>()
    val showPoweredByLiveData = MutableLiveData(false)

    val sortFields: Array<TvlRankSortField> =
        arrayOf(TvlRankSortField.HighestTvl, TvlRankSortField.LowestTvl)

    var sortField: TvlRankSortField = sortFields.first()
        private set

    val filterFields: Array<TvlRankFilterField> =
        arrayOf(
            TvlRankFilterField.All,
            TvlRankFilterField.Ethereum,
            TvlRankFilterField.Binance,
            TvlRankFilterField.Solana,
            TvlRankFilterField.Avalanche,
            TvlRankFilterField.Polygon
        )

    var filterField: TvlRankFilterField = filterFields.first()
        private set

    private var rawItemList: List<DefiTvl> = listOf()
    private val usdCurrency = appConfigProvider.currencies.first { it.code == "USD" }
    private var disposable: Disposable? = null

    init {
        getCoinList()
    }

    override fun onCleared() {
        disposable?.dispose()
    }

    fun changeSorting(sortField: TvlRankSortField) {
        this.sortField = sortField
        syncViewItems()
    }

    fun changeFilter(filterField: TvlRankFilterField) {
        this.filterField = filterField
        getCoinList()
    }

    private fun getCoinList() {
        coinList.postValue(emptyList())
        loadingLiveData.postValue(true)
        showPoweredByLiveData.postValue(false)

        xRateManager.getTopDefiTvlAsync(currencyCode = usdCurrency.code, chain = this.filterField.name.lowercase())
            .subscribeIO({ tvlRankList ->
                loadingLiveData.postValue(false)
                rawItemList = tvlRankList
                syncViewItems()
            }, {
                loadingLiveData.postValue(false)
                coinInfoErrorLiveData.postValue(Translator.getString(R.string.BalanceSyncError_Title))
            }).let {
                disposable = it
            }
    }

    private fun syncViewItems() {
        val sorted = rawItemList.sort(sortField)
        coinList.postValue(sorted.map { getViewItem(it) })
        showPoweredByLiveData.postValue(!sorted.isEmpty())
    }

    private fun getViewItem(defiTvl: DefiTvl): TvlRankViewItem {
        val shortCapValue = numberFormatter.shortenValue(defiTvl.tvl)
        val value = numberFormatter.formatFiat(
            shortCapValue.first,
            usdCurrency.symbol,
            0,
            2
        ) + " " + shortCapValue.second

        return TvlRankViewItem(
            defiTvl.data,
            value,
            defiTvl.tvlDiff,
            defiTvl.tvlRank.toString(),
            getChains(defiTvl.chains)
        )
    }

    private fun getChains(chains: List<String>?): String {
        return when {
            chains.isNullOrEmpty() -> ""
            chains.size > 1 -> Translator.getString(R.string.TvlRank_MultiChain)
            else -> chains[0]
        }
    }

    private fun List<DefiTvl>.sort(sortField: TvlRankSortField) = when (sortField) {
        TvlRankSortField.HighestTvl -> sortedByDescendingNullLast { it.tvl }
        TvlRankSortField.LowestTvl -> sortedByNullLast { it.tvl }
        //else -> throw IllegalArgumentException()
    }
}
