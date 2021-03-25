package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.ui.selector.ViewItemWrapper
import io.horizontalsystems.core.SingleLiveEvent
import io.reactivex.disposables.CompositeDisposable
import java.net.UnknownHostException

class MarketAdvancedSearchViewModel(
        val service: MarketAdvancedSearchService,
        private val clearables: List<Clearable>
) : ViewModel() {

    // Options
    val coinListsViewItemOptions = CoinList.values().map {
        ViewItemWrapper(App.instance.localizedContext().getString(it.titleResId), it, R.color.leah)
    }
    val marketCapViewItemOptions = ranges
    val volumeViewItemOptions = ranges
    val liquidityViewItemOptions = ranges
    val periodViewItemOptions = TimePeriod.values().map {
        ViewItemWrapper(App.instance.localizedContext().getString(it.titleResId), it, R.color.leah)
    }
    val priceChangeViewItemOptions = listOf(ViewItemWrapper.getAny<PriceChange>()) + PriceChange.values().map {
        ViewItemWrapper<PriceChange?>(App.instance.localizedContext().getString(it.titleResId), it, it.colorResId)
    }

    // ViewItem
    var coinListViewItem = ViewItemWrapper(App.instance.localizedContext().getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
        set(value) {
            field = value
            coinListViewItemLiveData.postValue(value)

            service.coinCount = value.item.itemsCount
        }
    var marketCapViewItem = rangeEmpty
        set(value) {
            field = value
            marketCapViewItemLiveData.postValue(value)

            service.filterMarketCap = value.item?.values
        }
    var volumeViewItem = rangeEmpty
        set(value) {
            field = value
            volumeViewItemLiveData.postValue(value)

            service.filterVolume = value.item?.values
        }
    var liquidityViewItem = rangeEmpty
        set(value) {
            field = value
            liquidityViewItemLiveData.postValue(value)

            service.filterLiquidity = value.item?.values
        }
    var periodViewItem = ViewItemWrapper(App.instance.localizedContext().getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        set(value) {
            field = value
            periodViewItemLiveData.postValue(value)

            service.filterPeriod = value.item.xRatesKitTimePeriod
        }
    var priceChangeViewItem: ViewItemWrapper<PriceChange?> = ViewItemWrapper.getAny()
        set(value) {
            field = value
            priceChangeViewItemLiveData.postValue(value)

            service.filterPriceChange = value.item?.values
        }

    // LiveData
    val coinListViewItemLiveData = MutableLiveData(coinListViewItem)
    val marketCapViewItemLiveData = MutableLiveData(marketCapViewItem)
    val volumeViewItemLiveData = MutableLiveData(volumeViewItem)
    val liquidityViewItemLiveData = MutableLiveData(liquidityViewItem)
    val periodViewItemLiveData = MutableLiveData(periodViewItem)
    val priceChangeViewItemLiveData = MutableLiveData(priceChangeViewItem)
    val showResultsTitleLiveData = MutableLiveData<String>()
    val showResultsEnabledLiveData = MutableLiveData(false)
    val errorLiveEvent = SingleLiveEvent<String>()
    val loadingLiveData = MutableLiveData(false)

    val disposable = CompositeDisposable()

    init {
        service.numberOfItemsAsync
                .subscribe {
                    val title = when (it) {
                        is DataState.Success -> App.instance.localizedContext().getString(R.string.Market_Filter_ShowResults_Counter, it.data)
                        is DataState.Error -> App.instance.localizedContext().getString(R.string.Market_Filter_ShowResults)
                        is DataState.Loading -> ""
                    }
                    showResultsTitleLiveData.postValue(title)

                    showResultsEnabledLiveData.postValue(it is DataState.Success && it.data > 0)
                    loadingLiveData.postValue(it is DataState.Loading)

                    it.errorOrNull?.let {
                        errorLiveEvent.postValue(convertErrorMessage(it))
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    fun reset() {
        coinListViewItem = ViewItemWrapper(App.instance.localizedContext().getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
        marketCapViewItem = ViewItemWrapper.getAny()
        volumeViewItem = ViewItemWrapper.getAny()
        liquidityViewItem = ViewItemWrapper.getAny()
        periodViewItem = ViewItemWrapper(App.instance.localizedContext().getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        priceChangeViewItem = ViewItemWrapper.getAny()
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    private fun convertErrorMessage(it: Throwable) = when (it) {
        is UnknownHostException -> App.instance.localizedContext().getString(R.string.Hud_Text_NoInternet)
        else -> it.message ?: it.javaClass.simpleName
    }
}

val rangeEmpty = ViewItemWrapper.getAny<Range>()
val ranges = listOf(rangeEmpty) + Range.values().map {
    ViewItemWrapper<Range?>(App.instance.localizedContext().getString(it.titleResId), it, R.color.leah)
}
