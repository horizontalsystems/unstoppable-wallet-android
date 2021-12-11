package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.providers.Translator
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
        ViewItemWrapper(Translator.getString(it.titleResId), it, R.color.leah)
    }
    val marketCapViewItemOptions = getRanges(service.currencyCode)
    val volumeViewItemOptions = getRanges(service.currencyCode)
    val periodViewItemOptions = TimePeriod.values().map {
        ViewItemWrapper(Translator.getString(it.titleResId), it, R.color.leah)
    }
    val priceChangeViewItemOptions = listOf(ViewItemWrapper.getAny<PriceChange>()) + PriceChange.values().map {
        ViewItemWrapper<PriceChange?>(Translator.getString(it.titleResId), it, it.colorResId)
    }

    // ViewItem
    var coinListViewItem = ViewItemWrapper(Translator.getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
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
    var periodViewItem = ViewItemWrapper(Translator.getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        set(value) {
            field = value
            periodViewItemLiveData.postValue(value)

            service.filterPeriod = value.item
        }
    var priceChangeViewItem: ViewItemWrapper<PriceChange?> = ViewItemWrapper.getAny()
        set(value) {
            field = value
            priceChangeViewItemLiveData.postValue(value)

            service.filterPriceChange = value.item?.values
        }
    var outperformedBtcOn = false
        set(value) {
            field = value
            outperformedBtcOnFilter.postValue(value)
            service.filterOutperformedBtcOn = value
        }
    var outperformedEthOn = false
        set(value) {
            field = value
            outperformedEthOnFilter.postValue(value)
            service.filterOutperformedEthOn = value
        }
    var outperformedBnbOn = false
        set(value) {
            field = value
            outperformedBnbOnFilter.postValue(value)
            service.filterOutperformedBnbOn = value
        }
    var priceCloseToAth = false
        set(value) {
            field = value
            priceCloseToAthFilter.postValue(value)
            service.filterPriceCloseToAth = value
        }
    var priceCloseToAtl = false
        set(value) {
            field = value
            priceCloseToAtlFilter.postValue(value)
            service.filterPriceCloseToAtl = value
        }

    // LiveData
    val coinListViewItemLiveData = MutableLiveData(coinListViewItem)
    val marketCapViewItemLiveData = MutableLiveData(marketCapViewItem)
    val volumeViewItemLiveData = MutableLiveData(volumeViewItem)
    val liquidityViewItemLiveData = MutableLiveData(liquidityViewItem)
    val periodViewItemLiveData = MutableLiveData(periodViewItem)
    val priceChangeViewItemLiveData = MutableLiveData(priceChangeViewItem)
    val updateResultButton = MutableLiveData<Triple<String, Boolean, Boolean>>()
    val errorLiveEvent = SingleLiveEvent<String>()
    val outperformedBtcOnFilter = MutableLiveData(false)
    val outperformedEthOnFilter = MutableLiveData(false)
    val outperformedBnbOnFilter = MutableLiveData(false)
    val priceCloseToAthFilter = MutableLiveData(false)
    val priceCloseToAtlFilter = MutableLiveData(false)

    val disposable = CompositeDisposable()

    init {
        service.numberOfItemsAsync
                .subscribe {
                    val title = when (it) {
                        is DataState.Success -> Translator.getString(R.string.Market_Filter_ShowResults_Counter, it.data)
                        is DataState.Error -> Translator.getString(R.string.Market_Filter_ShowResults)
                        is DataState.Loading -> ""
                    }
                    val showSpinner = it is DataState.Loading
                    val enabled = it is DataState.Success && it.data > 0
                    updateResultButton.postValue(Triple(title, showSpinner, enabled))

                    it.errorOrNull?.let {
                        errorLiveEvent.postValue(convertErrorMessage(it))
                    }
                }
                .let {
                    disposable.add(it)
                }
    }

    fun reset() {
        coinListViewItem = ViewItemWrapper(Translator.getString(CoinList.Top250.titleResId), CoinList.Top250, R.color.leah)
        marketCapViewItem = ViewItemWrapper.getAny()
        volumeViewItem = ViewItemWrapper.getAny()
        liquidityViewItem = ViewItemWrapper.getAny()
        periodViewItem = ViewItemWrapper(Translator.getString(TimePeriod.TimePeriod_1D.titleResId), TimePeriod.TimePeriod_1D, R.color.leah)
        priceChangeViewItem = ViewItemWrapper.getAny()
        outperformedBtcOn = false
        outperformedEthOn = false
        outperformedBnbOn = false
        priceCloseToAth = false
        priceCloseToAtl = false
    }

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }

    private fun convertErrorMessage(it: Throwable) = when (it) {
        is UnknownHostException -> Translator.getString(R.string.Hud_Text_NoInternet)
        else -> it.message ?: it.javaClass.simpleName
    }
}

val rangeEmpty = ViewItemWrapper.getAny<Range>()

fun getRanges(currencyCode: String): List<ViewItemWrapper<Range?>> {
    return listOf(rangeEmpty) + Range.valuesByCurrency(currencyCode).map {
        ViewItemWrapper(Translator.getString(it.titleResId), it, R.color.leah)
    }
}
