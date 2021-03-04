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
        ViewItemWrapper(App.instance.getString(it.titleResId), it, R.color.leah)
    }
    val marketCapViewItemOptions = ranges
    val volumeViewItemOptions = ranges
    val liquidityViewItemOptions = ranges
    val periodViewItemOptions = TimePeriod.values().map {
        ViewItemWrapper(App.instance.getString(it.titleResId), it, R.color.leah)
    }
    val priceChangeViewItemOptions = listOf(ViewItemWrapper.getNone<PriceChange>()) + PriceChange.values().map {
        ViewItemWrapper<PriceChange?>(App.instance.getString(it.titleResId), it, it.colorResId)
    }

    // ViewItem
    var coinListViewItem = ViewItemWrapper(App.instance.getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
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
    var periodViewItem = ViewItemWrapper(App.instance.getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        set(value) {
            field = value
            periodViewItemLiveData.postValue(value)

            service.filterPeriod = value.item.xRatesKitTimePeriod
        }
    var priceChangeViewItem: ViewItemWrapper<PriceChange?> = ViewItemWrapper.getNone()
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
    val numberOfItemsLiveData = MutableLiveData<Int?>()
    val showResultsEnabledLiveData = MutableLiveData(false)
    val errorLiveEvent = SingleLiveEvent<String>()

    val disposable = CompositeDisposable()

    init {
        service.numberOfItemsAsync
                .subscribe {
                    showResultsEnabledLiveData.postValue(it is DataState.Success && it.data > 0)
                    numberOfItemsLiveData.postValue(it.dataOrNull)

                    it.errorOrNull?.let {
                        errorLiveEvent.postValue(convertErrorMessage(it))
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    fun reset() {
        coinListViewItem = ViewItemWrapper(App.instance.getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
        marketCapViewItem = ViewItemWrapper.getNone()
        volumeViewItem = ViewItemWrapper.getNone()
        liquidityViewItem = ViewItemWrapper.getNone()
        periodViewItem = ViewItemWrapper(App.instance.getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        priceChangeViewItem = ViewItemWrapper.getNone()
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    private fun convertErrorMessage(it: Throwable) = when (it) {
        is UnknownHostException -> App.instance.getString(R.string.Hud_Text_NoInternet)
        else -> it.message ?: it.javaClass.simpleName
    }
}

val rangeEmpty = ViewItemWrapper.getNone<Range>()
val ranges = listOf(rangeEmpty) + Range.values().map {
    ViewItemWrapper<Range?>(App.instance.getString(it.titleResId), it, R.color.leah)
}
