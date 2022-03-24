package io.horizontalsystems.bankwallet.modules.market.advancedsearch

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchModule.FilterDropdown.*
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchModule.FilterSwitch.*
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchModule.Item.DropDown
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchModule.Item.Switch
import io.horizontalsystems.bankwallet.modules.market.advancedsearch.MarketAdvancedSearchModule.Section
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.reactivex.disposables.CompositeDisposable
import java.net.UnknownHostException

class MarketAdvancedSearchViewModel(
    val service: MarketAdvancedSearchService,
) : ViewModel() {

    private val sections: List<Section>
        get() = listOf(
            Section(
                null,
                listOf(
                    DropDown(CoinSet, coinListViewItem.title)
                )
            ),
            Section(
                R.string.Market_FilterSection_MarketParameters,
                listOf(
                    DropDown(MarketCap, marketCapViewItem.title),
                    DropDown(TradingVolume, volumeViewItem.title),
                )
            ),
            Section(
                R.string.Market_FilterSection_NetworkParameters,
                listOf(
                    DropDown(Blockchain, blockchainFilterText),
                )
            ),
            Section(
                R.string.Market_FilterSection_PriceParameters,
                listOf(
                    DropDown(PriceChange, priceChangeViewItem.title),
                    DropDown(PricePeriod, periodViewItem.title),
                    Switch(OutperformedBtc, outperformedBtcOn),
                    Switch(OutperformedEth, outperformedEthOn),
                    Switch(OutperformedBnb, outperformedBnbOn),
                    Switch(PriceCloseToAth, priceCloseToAth),
                    Switch(PriceCloseToAtl, priceCloseToAtl),
                )
            ),
        )

    val blockchainFilterText: String?
        get() = if (selectedBlockchainIndexes.isEmpty()) null else selectedBlockchainIndexes.size.toString()

    // Options
    val coinListsViewItemOptions = CoinList.values().map {
        FilterViewItemWrapper(Translator.getString(it.titleResId), it)
    }
    val marketCapViewItemOptions = getRanges(service.currencyCode)
    val volumeViewItemOptions = getRanges(service.currencyCode)
    val periodViewItemOptions = TimePeriod.values().map {
        FilterViewItemWrapper(Translator.getString(it.titleResId), it)
    }
    val priceChangeViewItemOptions =
        listOf(FilterViewItemWrapper.getAny<PriceChange>()) + PriceChange.values().map {
            FilterViewItemWrapper<PriceChange?>(Translator.getString(it.titleResId), it)
        }
    val blockchainOptions = MarketAdvancedSearchModule.Blockchain.values().map {
        FilterViewItemWrapper(it.value, it)
    }

    // ViewItem
    var coinListViewItem = FilterViewItemWrapper(
        Translator.getString(CoinList.Top250.titleResId),
        CoinList.Top250,
    )
        set(value) {
            field = value
            updateItems()
            service.coinCount = value.item.itemsCount
        }

    var marketCapViewItem = rangeEmpty
        set(value) {
            field = value
            updateItems()
            service.filterMarketCap = value.item?.values
        }
    var volumeViewItem = rangeEmpty
        set(value) {
            field = value
            updateItems()
            service.filterVolume = value.item?.values
        }
    var liquidityViewItem = rangeEmpty
        set(value) {
            field = value
            updateItems()
            service.filterLiquidity = value.item?.values
        }
    var selectedBlockchainIndexes = listOf<Int>()
        set(value) {
            field = value
            updateItems()
            val selectedBlockchains =
                value.map { MarketAdvancedSearchModule.Blockchain.values()[it] }
            service.filterBlockchains = selectedBlockchains
        }

    var periodViewItem = FilterViewItemWrapper(
        Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
        TimePeriod.TimePeriod_1D,
    )
        set(value) {
            field = value
            updateItems()
            service.filterPeriod = value.item
        }
    var priceChangeViewItem: FilterViewItemWrapper<PriceChange?> = FilterViewItemWrapper.getAny()
        set(value) {
            field = value
            updateItems()
            service.filterPriceChange = value.item?.values
        }
    private var outperformedBtcOn = false
        set(value) {
            field = value
            updateItems()
            service.filterOutperformedBtcOn = value
        }
    private var outperformedEthOn = false
        set(value) {
            field = value
            updateItems()
            service.filterOutperformedEthOn = value
        }
    private var outperformedBnbOn = false
        set(value) {
            field = value
            updateItems()
            service.filterOutperformedBnbOn = value
        }
    private var priceCloseToAth = false
        set(value) {
            field = value
            updateItems()
            service.filterPriceCloseToAth = value
        }
    private var priceCloseToAtl = false
        set(value) {
            field = value
            updateItems()
            service.filterPriceCloseToAtl = value
        }

    private fun updateItems() {
        sectionsLiveData.postValue(sections)
    }

    // LiveData
    val sectionsLiveData = MutableLiveData(sections)

    val disposable = CompositeDisposable()

    var showSpinner by mutableStateOf(false)
        private set

    var buttonEnabled by mutableStateOf(false)
        private set

    var buttonTitle by mutableStateOf(Translator.getString(R.string.Market_Filter_ShowResults))
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    init {
        service.numberOfItemsAsync
            .subscribe {
                val title = when (it) {
                    is DataState.Success -> Translator.getString(
                        R.string.Market_Filter_ShowResults_Counter,
                        it.data
                    )
                    is DataState.Error -> Translator.getString(R.string.Market_Filter_ShowResults)
                    is DataState.Loading -> ""
                }
                buttonTitle = title
                showSpinner = it is DataState.Loading
                buttonEnabled = it is DataState.Success && it.data > 0

                errorMessage = it.errorOrNull?.let { error -> convertErrorMessage(error) }
            }
            .let {
                disposable.add(it)
            }

        service.start()
    }

    fun reset() {
        coinListViewItem = FilterViewItemWrapper(
            Translator.getString(CoinList.Top250.titleResId),
            CoinList.Top250,
        )
        marketCapViewItem = rangeEmpty
        volumeViewItem = rangeEmpty
        liquidityViewItem = rangeEmpty
        periodViewItem = FilterViewItemWrapper(
            Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
            TimePeriod.TimePeriod_1D,
        )
        priceChangeViewItem = FilterViewItemWrapper.getAny()
        outperformedBtcOn = false
        outperformedEthOn = false
        outperformedBnbOn = false
        priceCloseToAth = false
        priceCloseToAtl = false
        selectedBlockchainIndexes = listOf()
    }

    override fun onCleared() {
        disposable.clear()
        service.stop()
    }

    fun onSwitchChanged(type: MarketAdvancedSearchModule.FilterSwitch, enabled: Boolean) {
        when (type) {
            OutperformedBtc -> outperformedBtcOn = enabled
            OutperformedEth -> outperformedEthOn = enabled
            OutperformedBnb -> outperformedBnbOn = enabled
            PriceCloseToAth -> priceCloseToAth = enabled
            PriceCloseToAtl -> priceCloseToAtl = enabled
        }
    }

    private fun convertErrorMessage(error: Throwable) = when (error) {
        is UnknownHostException -> TranslatableString.ResString(R.string.Hud_Text_NoInternet)
        else -> TranslatableString.PlainString(error.message ?: error.javaClass.simpleName)
    }
}

val rangeEmpty = FilterViewItemWrapper.getAny<Range>()

fun getRanges(currencyCode: String): List<FilterViewItemWrapper<Range?>> {
    return listOf(rangeEmpty) + Range.valuesByCurrency(currencyCode).map {
        FilterViewItemWrapper(Translator.getString(it.titleResId), it)
    }
}
