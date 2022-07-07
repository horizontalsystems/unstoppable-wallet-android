package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.collectWith
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.BlockchainViewItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class MarketFiltersViewModel(val service: MarketFiltersService) : ViewModel() {

    var coinListSet by mutableStateOf(
        FilterViewItemWrapper(
            Translator.getString(CoinList.Top250.titleResId),
            CoinList.Top250,
        )
    )
        private set

    var period by mutableStateOf(
        FilterViewItemWrapper(
            Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
            TimePeriod.TimePeriod_1D,
        )
    )
        private set

    var marketCap by mutableStateOf(rangeEmpty)
        private set
    var volume by mutableStateOf(rangeEmpty)
        private set
    var priceChange by mutableStateOf<FilterViewItemWrapper<PriceChange?>>(FilterViewItemWrapper.getAny())
        private set
    var outperformedBtcOn by mutableStateOf(false)
        private set
    var outperformedEthOn by mutableStateOf(false)
        private set
    var outperformedBnbOn by mutableStateOf(false)
        private set
    var priceCloseToAth by mutableStateOf(false)
        private set
    var priceCloseToAtl by mutableStateOf(false)
        private set
    var selectedBlockchainsValue by mutableStateOf<String?>(null)
        private set

    var selectedBlockchainIndexes by mutableStateOf(listOf<Int>())

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

    var blockchainOptions by mutableStateOf<List<BlockchainViewItem>>(emptyList())
        private set

    var showSpinner by mutableStateOf(false)
        private set

    var buttonEnabled by mutableStateOf(false)
        private set

    var buttonTitle by mutableStateOf(Translator.getString(R.string.Market_Filter_ShowResults))
        private set

    var errorMessage by mutableStateOf<TranslatableString?>(null)
        private set

    private var result: Result<Int>? = null

    init {
        service.numberOfItems.collectWith(viewModelScope) {
            result = it
            sync()
        }

        updateSelectedBlockchains()
        refresh()
    }

    private fun sync() {
        showSpinner = false

        buttonTitle = result?.getOrNull()?.let {
            Translator.getString(R.string.Market_Filter_ShowResults_Counter, it)
        } ?: result?.exceptionOrNull()?.let {
            Translator.getString(R.string.Market_Filter_ShowResults)
        } ?: ""

        buttonEnabled = result?.getOrNull()?.let { it > 0 } ?: false

        errorMessage = result?.exceptionOrNull()?.let { convertErrorMessage(it) }
    }

    fun reset() {
        updateCoinList(
            FilterViewItemWrapper(
                Translator.getString(CoinList.Top250.titleResId),
                CoinList.Top250,
            )
        )
        marketCap = rangeEmpty
        volume = rangeEmpty
        period = FilterViewItemWrapper(
            Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
            TimePeriod.TimePeriod_1D,
        )
        priceChange = FilterViewItemWrapper.getAny()
        outperformedBtcOn = false
        outperformedEthOn = false
        outperformedBnbOn = false
        priceCloseToAth = false
        priceCloseToAtl = false
        selectedBlockchainIndexes = listOf()
        service.filterBlockchains = emptyList()
        updateSelectedBlockchains()
        refresh()
    }

    fun updateCoinList(value: FilterViewItemWrapper<CoinList>) {
        coinListSet = value
        service.coinCount = value.item.itemsCount
        service.clearCache()
        refresh()
    }

    fun updateMarketCap(value: FilterViewItemWrapper<Range?>) {
        marketCap = value
        service.filterMarketCap = value.item?.values
        refresh()
    }

    fun updateVolume(value: FilterViewItemWrapper<Range?>) {
        volume = value
        service.filterVolume = value.item?.values
        refresh()
    }

    fun updatePeriod(value: FilterViewItemWrapper<TimePeriod>) {
        period = value
        service.filterPeriod = value.item
        refresh()
    }

    fun updatePriceChange(value: FilterViewItemWrapper<PriceChange?>) {
        priceChange = value
        service.filterPriceChange = value.item?.values
        refresh()
    }

    fun onBlockchainCheck(blockchain: Blockchain) {
        val index = service.blockchains.indexOf(blockchain)
        selectedBlockchainIndexes = selectedBlockchainIndexes.toMutableList().also {
            it.add(index)
        }
        updateSelectedBlockchains()
    }

    fun onBlockchainUncheck(blockchain: Blockchain) {
        val index = service.blockchains.indexOf(blockchain)
        selectedBlockchainIndexes = selectedBlockchainIndexes.toMutableList().also {
            it.remove(index)
        }
        updateSelectedBlockchains()
    }

    fun updateListBySelectedBlockchains() {
        val selectedBlockchains = selectedBlockchainIndexes.map { service.blockchains[it] }
        service.filterBlockchains = selectedBlockchains
        refresh()
    }

    fun updateOutperformedBtcOn(checked: Boolean) {
        outperformedBtcOn = checked
        service.filterOutperformedBtcOn = checked
        refresh()
    }

    fun updateOutperformedEthOn(checked: Boolean) {
        outperformedEthOn = checked
        service.filterOutperformedEthOn = checked
        refresh()
    }

    fun updateOutperformedBnbOn(checked: Boolean) {
        outperformedBnbOn = checked
        service.filterOutperformedBnbOn = checked
        refresh()
    }

    fun updateOutperformedAthOn(checked: Boolean) {
        priceCloseToAth = checked
        service.filterPriceCloseToAth = checked
        refresh()
    }

    fun updateOutperformedAtlOn(checked: Boolean) {
        priceCloseToAtl = checked
        service.filterPriceCloseToAtl = checked
        refresh()
    }

    fun anyBlockchains() {
        selectedBlockchainIndexes = emptyList()
        updateSelectedBlockchains()
        refresh()
    }

    private fun refresh() {
        showSpinner = true
        viewModelScope.launch {
            service.refresh()
        }
    }

    private fun updateSelectedBlockchains() {
        blockchainOptions = service.blockchains.mapIndexed { index, blockchain ->
            BlockchainViewItem(
                blockchain,
                selectedBlockchainIndexes.contains(index)
            )
        }
        selectedBlockchainsValue =
            if (selectedBlockchainIndexes.isEmpty()) null else selectedBlockchainIndexes.size.toString()
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
