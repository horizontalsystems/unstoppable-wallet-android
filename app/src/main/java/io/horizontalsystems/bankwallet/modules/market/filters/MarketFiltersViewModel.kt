package io.horizontalsystems.bankwallet.modules.market.filters

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.modules.market.filters.MarketFiltersModule.BlockchainViewItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.marketkit.models.Blockchain
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.net.UnknownHostException

class MarketFiltersViewModel(val service: MarketFiltersService) :
    ViewModelUiState<MarketFiltersUiState>() {

    private var coinListSet: CoinList = CoinList.Top100
    private var period = FilterViewItemWrapper(
        Translator.getString(TimePeriod.TimePeriod_1D.titleResId),
        TimePeriod.TimePeriod_1D,
    )
    private var filterTradingSignal = FilterViewItemWrapper.getAny<FilterTradingSignal>()
    private var marketCap = rangeEmpty
    private var volume = rangeEmpty
    private var priceChange = FilterViewItemWrapper.getAny<PriceChange>()
    private var selectedSectors: List<FilterViewItemWrapper<SectorItem?>> =
        listOf(FilterViewItemWrapper.getAny())
    private var priceCloseTo: PriceCloseTo? = null
    private var outperformedBtcOn = false
    private var outperformedEthOn = false
    private var outperformedBnbOn = false
    private var outperformedGoldOn = false
    private var outperformedSnpOn = false
    private var listedOnTopExchangesOn = false
    private var solidCexOn = false
    private var solidDexOn = false
    private var goodDistributionOn = false
    private var selectedBlockchainsValue: String? = null
    private var selectedBlockchains = listOf<Blockchain>()
    private var blockchainOptions = listOf<BlockchainViewItem>()
    private var showSpinner = false
    private var buttonEnabled = false
    private var buttonTitle = Translator.getString(R.string.Market_Filter_ShowResults)
    private var errorMessage: TranslatableString? = null
    private var sectors: List<SectorItem> = listOf()
    private var resetEnabled = false

    private var reloadDataJob: Job? = null

    val sectorsViewItemOptions: List<FilterViewItemWrapper<SectorItem?>>
        get() = listOf(FilterViewItemWrapper.getAny<SectorItem>()) + sectors.map { FilterViewItemWrapper(it.title, it) }

    val coinListsViewItemOptions = CoinList.entries
    val marketCapViewItemOptions = getRanges(service.currencyCode)
    val volumeViewItemOptions = getRanges(service.currencyCode)
    val periodViewItemOptions = TimePeriod.values().map {
        FilterViewItemWrapper(Translator.getString(it.titleResId), it)
    }
    val priceCloseToOptions = PriceCloseTo.entries

    val tradingSignals = listOf(FilterViewItemWrapper.getAny<FilterTradingSignal>()) +
                FilterTradingSignal.values().map { FilterViewItemWrapper<FilterTradingSignal?>(Translator.getString(it.titleResId), it) }
    val priceChangeViewItemOptions =
        listOf(FilterViewItemWrapper.getAny<PriceChange>()) + PriceChange.values().map {
            FilterViewItemWrapper<PriceChange?>(Translator.getString(it.titleResId), it)
        }

    init {
        updateSelectedBlockchains()
        reloadDataWithSpinner()
        loadSectors()
    }

    private fun loadSectors() {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                sectors = service.getSectors()
            } catch (e: Throwable) {
                //not handled
            }
        }
    }

    override fun createState() = MarketFiltersUiState(
        coinListSet = coinListSet,
        period = period,
        marketCap = marketCap,
        volume = volume,
        priceChange = priceChange,
        sectors = selectedSectors,
        priceCloseTo = priceCloseTo,
        outperformedBtcOn = outperformedBtcOn,
        outperformedEthOn = outperformedEthOn,
        outperformedBnbOn = outperformedBnbOn,
        outperformedGoldOn = outperformedGoldOn,
        outperformedSnpOn = outperformedSnpOn,
        selectedBlockchainsValue = selectedBlockchainsValue,
        selectedBlockchains = selectedBlockchains,
        blockchainOptions = blockchainOptions,
        showSpinner = showSpinner,
        buttonEnabled = buttonEnabled,
        buttonTitle = buttonTitle,
        errorMessage = errorMessage,
        listedOnTopExchangesOn = listedOnTopExchangesOn,
        solidCexOn = solidCexOn,
        solidDexOn = solidDexOn,
        goodDistributionOn = goodDistributionOn,
        filterTradingSignal = filterTradingSignal,
        resetEnabled = resetEnabled,
    )

    fun reset() {
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
        outperformedGoldOn = false
        outperformedSnpOn = false
        listedOnTopExchangesOn = false
        solidCexOn = false
        solidDexOn = false
        goodDistributionOn = false
        selectedBlockchains = emptyList()
        filterTradingSignal = FilterViewItemWrapper.getAny()
        updateSelectedBlockchains()

        selectedSectors = listOf(FilterViewItemWrapper.getAny())
        service.sectorIds = emptyList()
        coinListSet = CoinList.Top100
        service.coinCount = CoinList.Top100.itemsCount
        resetEnabled = false
        reloadDataWithSpinner()
    }

    fun updateCoinList(value: CoinList) {
        coinListSet = value
        service.coinCount = value.itemsCount
        resetEnabled = true
        reloadDataWithSpinner()
    }

    fun setSectors(sectorItems: List<FilterViewItemWrapper<SectorItem?>>) {
        resetEnabled = true
        if (sectorItems.isEmpty()) {
            selectedSectors = listOf(FilterViewItemWrapper.getAny())
            service.sectorIds = emptyList()
            reloadData()
            return
        }
        selectedSectors = sectorItems
        service.sectorIds = sectorItems.mapNotNull { it.item?.id }
        reloadData()
    }

    private fun reloadDataWithSpinner() {
        service.clearCache()
        showSpinner = true
        emitState()
        reloadData()
    }

    fun updatePriceCloseTo(value: PriceCloseTo?) {
        resetEnabled = true
        priceCloseTo = value

        emitState()
        reloadData()
    }

    fun updateMarketCap(value: FilterViewItemWrapper<Range?>) {
        resetEnabled = true
        marketCap = value
        emitState()
        reloadData()
    }

    fun updateVolume(value: FilterViewItemWrapper<Range?>) {
        resetEnabled = true
        volume = value
        emitState()
        reloadData()
    }

    fun updatePeriod(value: FilterViewItemWrapper<TimePeriod>) {
        resetEnabled = true
        period = value
        emitState()
        reloadData()
    }

    fun updateTradingSignal(value: FilterViewItemWrapper<FilterTradingSignal?>) {
        resetEnabled = true
        filterTradingSignal = value
        emitState()
        reloadData()
    }

    fun updatePriceChange(value: FilterViewItemWrapper<PriceChange?>) {
        resetEnabled = true
        priceChange = value
        emitState()
        reloadData()
    }

    fun updateOutperformedBtcOn(checked: Boolean) {
        resetEnabled = true
        outperformedBtcOn = checked
        emitState()
        reloadData()
    }

    fun updateOutperformedEthOn(checked: Boolean) {
        resetEnabled = true
        outperformedEthOn = checked
        emitState()
        reloadData()
    }

    fun updateOutperformedBnbOn(checked: Boolean) {
        resetEnabled = true
        outperformedBnbOn = checked
        emitState()
        reloadData()
    }

    fun updateOutperformedGoldOn(checked: Boolean) {
        resetEnabled = true
        outperformedGoldOn = checked
        emitState()
        reloadData()
    }

    fun updateOutperformedSnpOn(checked: Boolean) {
        resetEnabled = true
        outperformedSnpOn = checked
        emitState()
        reloadData()
    }

    fun updateListedOnTopExchangesOn(checked: Boolean) {
        resetEnabled = true
        listedOnTopExchangesOn = checked
        emitState()
        reloadData()
    }

    fun updateSolidCexOn(checked: Boolean) {
        resetEnabled = true
        solidCexOn = checked
        emitState()
        reloadData()
    }

    fun updateSolidDexOn(checked: Boolean) {
        resetEnabled = true
        solidDexOn = checked
        emitState()
        reloadData()
    }

    fun updateGoodDistributionOn(checked: Boolean) {
        resetEnabled = true
        goodDistributionOn = checked
        emitState()
        reloadData()
    }

    fun anyBlockchains() {
        selectedBlockchains = emptyList()
        updateSelectedBlockchains()
        reloadData()
    }

    fun onBlockchainCheck(blockchain: Blockchain) {
        resetEnabled = true
        selectedBlockchains += blockchain
        updateSelectedBlockchains()
        reloadData()
    }

    fun onBlockchainUncheck(blockchain: Blockchain) {
        resetEnabled = true
        selectedBlockchains -= blockchain
        updateSelectedBlockchains()
        reloadData()
    }

    private fun reloadData() {
        reloadDataJob?.cancel()
        reloadDataJob = viewModelScope.launch(Dispatchers.Default) {
            try {
                service.filterMarketCap = marketCap.item?.values
                service.filterVolume = volume.item?.values
                service.filterPeriod = period.item
                service.filterPriceChange = priceChange.item?.values
                service.filterOutperformedBtcOn = outperformedBtcOn
                service.filterOutperformedEthOn = outperformedEthOn
                service.filterOutperformedBnbOn = outperformedBnbOn
                service.filterOutperformedGoldOn = outperformedGoldOn
                service.filterOutperformedSnpOn = outperformedSnpOn
                service.filterListedOnTopExchanges = listedOnTopExchangesOn
                service.filterSolidCex = solidCexOn
                service.filterSolidDex = solidDexOn
                service.filterGoodDistribution = goodDistributionOn
                service.filterPriceCloseToAth = priceCloseTo == PriceCloseTo.Ath
                service.filterPriceCloseToAtl = priceCloseTo == PriceCloseTo.Atl
                service.filterBlockchains = selectedBlockchains
                service.filterTradingSignal = filterTradingSignal.item?.getAdvices() ?: emptyList()

                if ((outperformedSnpOn || outperformedGoldOn) && service.goldPriceChanges == null) {
                    service.setStockPriceChanges()
                }

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
    val coinListSet: CoinList,
    val period: FilterViewItemWrapper<TimePeriod>,
    val filterTradingSignal: FilterViewItemWrapper<FilterTradingSignal?>,
    val marketCap: FilterViewItemWrapper<Range?>,
    val volume: FilterViewItemWrapper<Range?>,
    val priceChange: FilterViewItemWrapper<PriceChange?>,
    val sectors: List<FilterViewItemWrapper<SectorItem?>>,
    val priceCloseTo: PriceCloseTo?,
    val outperformedBtcOn: Boolean,
    val outperformedEthOn: Boolean,
    val outperformedBnbOn: Boolean,
    val outperformedGoldOn: Boolean,
    val outperformedSnpOn: Boolean,
    val selectedBlockchainsValue: String?,
    val selectedBlockchains: List<Blockchain>,
    val blockchainOptions: List<BlockchainViewItem>,
    val showSpinner: Boolean,
    val buttonEnabled: Boolean,
    val buttonTitle: String,
    val errorMessage: TranslatableString?,
    val listedOnTopExchangesOn: Boolean,
    val solidCexOn: Boolean,
    val solidDexOn: Boolean,
    val goodDistributionOn: Boolean,
    val resetEnabled: Boolean
)
