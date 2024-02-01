package cash.p.terminal.modules.market.filters

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.R
import cash.p.terminal.core.providers.Translator
import cash.p.terminal.modules.market.filters.MarketFiltersModule.BlockchainViewItem
import cash.p.terminal.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class MarketFiltersViewModel(val service: MarketFiltersService) : ViewModel() {
    private var coinListSet = FilterViewItemWrapper(
        Translator.getString(CoinList.Top250.titleResId),
        CoinList.Top250,
    )
    private var period = FilterViewItemWrapper(
        Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
        TimePeriod.TimePeriod_1D,
    )
    private var marketCap = rangeEmpty
    private var volume = rangeEmpty
    private var priceChange = FilterViewItemWrapper.getAny<PriceChange>()
    private var outperformedBtcOn = false
    private var outperformedEthOn = false
    private var outperformedBnbOn = false
    private var priceCloseToAth = false
    private var priceCloseToAtl = false
    private var selectedBlockchainsValue: String? = null
    private var selectedBlockchains = listOf<Blockchain>()
    private var blockchainOptions = listOf<BlockchainViewItem>()
    private var showSpinner = false
    private var buttonEnabled = false
    private var buttonTitle = Translator.getString(R.string.Market_Filter_ShowResults)
    private var errorMessage: TranslatableString? = null

    var uiState by mutableStateOf(
        MarketFiltersUiState(
            coinListSet = coinListSet,
            period = period,
            marketCap = marketCap,
            volume = volume,
            priceChange = priceChange,
            outperformedBtcOn = outperformedBtcOn,
            outperformedEthOn = outperformedEthOn,
            outperformedBnbOn = outperformedBnbOn,
            priceCloseToAth = priceCloseToAth,
            priceCloseToAtl = priceCloseToAtl,
            selectedBlockchainsValue = selectedBlockchainsValue,
            selectedBlockchains = selectedBlockchains,
            blockchainOptions = blockchainOptions,
            showSpinner = showSpinner,
            buttonEnabled = buttonEnabled,
            buttonTitle = buttonTitle,
            errorMessage = errorMessage,
        )
    )
        private set

    private var fetchDataJob: Job? = null

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

    init {
        updateSelectedBlockchains()
        refresh()
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
        selectedBlockchains = emptyList()
        updateSelectedBlockchains()
        resetService()
        refresh()
    }

    private fun resetService() {
        service.filterMarketCap = marketCap.item?.values
        service.filterVolume = volume.item?.values
        service.filterPeriod = period.item
        service.filterPriceChange = priceChange.item?.values
        service.filterOutperformedBtcOn = outperformedBtcOn
        service.filterOutperformedEthOn = outperformedEthOn
        service.filterOutperformedBnbOn = outperformedBnbOn
        service.filterPriceCloseToAth = priceCloseToAth
        service.filterPriceCloseToAtl = priceCloseToAtl
        service.filterBlockchains = selectedBlockchains
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
        selectedBlockchains = selectedBlockchains.toMutableList()
            .also {
                it.add(blockchain)
            }
        updateSelectedBlockchains()
    }

    fun onBlockchainUncheck(blockchain: Blockchain) {
        selectedBlockchains = selectedBlockchains.toMutableList()
            .also {
                it.remove(blockchain)
            }
        updateSelectedBlockchains()
    }

    fun updateListBySelectedBlockchains() {
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
        selectedBlockchains = emptyList()
        updateSelectedBlockchains()
        refresh()
    }

    private fun refresh() {
        fetchDataJob?.cancel()
        showSpinner = true
        buttonEnabled = false
        emitState()

        fetchDataJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                val numberOfItems = service.fetchNumberOfItems()

                buttonTitle = Translator.getString(R.string.Market_Filter_ShowResults_Counter, numberOfItems)
                buttonEnabled = numberOfItems > 0
                errorMessage = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                buttonTitle = Translator.getString(R.string.Market_Filter_ShowResults)
                buttonEnabled = false
                errorMessage = convertErrorMessage(e)
            }

            showSpinner = false

            ensureActive()
            emitState()
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = MarketFiltersUiState(
                coinListSet = coinListSet,
                period = period,
                marketCap = marketCap,
                volume = volume,
                priceChange = priceChange,
                outperformedBtcOn = outperformedBtcOn,
                outperformedEthOn = outperformedEthOn,
                outperformedBnbOn = outperformedBnbOn,
                priceCloseToAth = priceCloseToAth,
                priceCloseToAtl = priceCloseToAtl,
                selectedBlockchainsValue = selectedBlockchainsValue,
                selectedBlockchains = selectedBlockchains,
                blockchainOptions = blockchainOptions,
                showSpinner = showSpinner,
                buttonEnabled = buttonEnabled,
                buttonTitle = buttonTitle,
                errorMessage = errorMessage,
            )
        }
    }

    private fun updateSelectedBlockchains() {
        blockchainOptions = service.blockchains.map { blockchain ->
            BlockchainViewItem(blockchain, selectedBlockchains.contains(blockchain))
        }
        selectedBlockchainsValue =
            if (selectedBlockchains.isEmpty()) null else selectedBlockchains.size.toString()
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

data class MarketFiltersUiState(
    val coinListSet: FilterViewItemWrapper<CoinList>,
    val period: FilterViewItemWrapper<TimePeriod>,
    val marketCap: FilterViewItemWrapper<Range?>,
    val volume: FilterViewItemWrapper<Range?>,
    val priceChange: FilterViewItemWrapper<PriceChange?>,
    val outperformedBtcOn: Boolean,
    val outperformedEthOn: Boolean,
    val outperformedBnbOn: Boolean,
    val priceCloseToAth: Boolean,
    val priceCloseToAtl: Boolean,
    val selectedBlockchainsValue: String?,
    val selectedBlockchains: List<Blockchain>,
    val blockchainOptions: List<BlockchainViewItem>,
    val showSpinner: Boolean,
    val buttonEnabled: Boolean,
    val buttonTitle: String,
    val errorMessage: TranslatableString?
)
