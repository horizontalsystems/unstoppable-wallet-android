package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.chartview.models.ChartIndicator
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.LatestRate
import io.horizontalsystems.xrateskit.entities.TimePeriod
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class CoinViewModel(
        val rateFormatter: RateFormatter,
        private val service: CoinService,
        val coinCode: String,
        private val coinTitle: String,
        private val factory: CoinViewFactory,
        private val clearables: List<Clearable>
) : ViewModel() {

    val chartSpinner = MutableLiveData<Boolean>()
    val marketSpinner = MutableLiveData<Boolean>()
    val setDefaultMode = MutableLiveData<ChartType>()
    val setSelectedPoint = MutableLiveData<ChartPointViewItem>()
    val showChartInfo = MutableLiveData<ChartInfoViewItem>()
    val coinDetailsLiveData = MutableLiveData<CoinDetailsViewItem>()
    val showChartError = MutableLiveData<Unit>()
    val showEma = MutableLiveData<Boolean>()
    val showMacd = MutableLiveData<Boolean>()
    val showRsi = MutableLiveData<Boolean>()
    val setChartIndicatorsEnabled = MutableLiveData<Boolean>()
    val alertNotificationUpdated = MutableLiveData<Unit>()
    val showNotificationMenu = SingleLiveEvent<Pair<CoinType, String>>()
    val isFavorite = MutableLiveData<Boolean>()
    val coinMarkets = MutableLiveData<List<MarketTickerViewItem>>()
    val coinInvestors = MutableLiveData<List<InvestorItem>>()
    val extraPages = MutableLiveData<List<CoinExtraPage>>()
    val uncheckIndicators = MutableLiveData<List<ChartIndicator>>()
    val latestRateLiveData = MutableLiveData<CurrencyValue>()

    var notificationIconVisible = service.notificationsAreEnabled && service.notificationSupported
    var notificationIconActive = false

    private var enabledIndicator: ChartIndicator? = null
    private var macdIsEnabled = false
    private val disposable = CompositeDisposable()

    private val rateDiffCoinCodes: List<String> = mutableListOf(service.currency.code).apply {
        if (service.coinType != CoinType.Bitcoin) add("BTC")
        if (service.coinType != CoinType.Ethereum) add("ETH")
    }

    init {
        setDefaultMode.postValue(service.chartType)
        updateChartIndicatorState()

        service.getCoinDetails(rateDiffCoinCodes, listOf(TimePeriod.DAY_7, TimePeriod.DAY_30))

        fetchChartInfo()

        updateAlertNotificationIconState()

        updateFavoriteNotificationItemState()

        service.latestRateAsync
                .subscribeIO {
                    updateLatestRate(it)
                }
                .let {
                    disposable.add(it)
                }

        service.chartInfoUpdatedObservable
                .subscribeIO {
                    updateChartInfo()
                }
                .let {
                    disposable.add(it)
                }

        service.chartInfoErrorObservable
                .subscribeIO {
                    onChartError(it)
                }
                .let {
                    disposable.add(it)
                }

        service.coinDetailsStateObservable
                .subscribeIO {
                    syncCoinDetailsState(it)
                }
                .let {
                    disposable.add(it)
                }

        service.alertNotificationUpdatedObservable
                .subscribeIO {
                    updateAlertNotificationIconState()
                }
                .let {
                    disposable.add(it)
                }
    }

    private fun updateLatestRate(latestRate: LatestRate) {
        latestRateLiveData.postValue(CurrencyValue(service.currency, latestRate.rate))
    }

    fun onSelect(type: ChartType) {
        if (service.chartType == type)
            return

        service.chartType = type

        updateChartIndicatorState()

        fetchChartInfo()
    }

    fun onTouchSelect(point: PointInfo) {
        val price = CurrencyValue(service.currency, point.value.toBigDecimal())

        if (macdIsEnabled) {
            setSelectedPoint.postValue(ChartPointViewItem(point.timestamp, price, null, point.macdInfo))
        } else {
            val volume = point.volume?.let { volume ->
                CurrencyValue(service.currency, volume.toBigDecimal())
            }
            setSelectedPoint.postValue(ChartPointViewItem(point.timestamp, price, volume, null))
        }
    }

    fun onNotificationClick() {
        showNotificationMenu.postValue(Pair(service.coinType, coinTitle))
    }

    fun onFavoriteClick() {
        service.favorite()
        updateFavoriteNotificationItemState()
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
        updateFavoriteNotificationItemState()
    }

    fun setIndicatorChanged(indicator: ChartIndicator, checked: Boolean) {
        enabledIndicator = if (checked) indicator else null

        if (checked) {
            val itemsToUncheck = ChartIndicator.values().filter { it != indicator }
            uncheckIndicators.postValue(itemsToUncheck)
        }
        when (indicator) {
            ChartIndicator.Ema -> showEma.postValue(checked)
            ChartIndicator.Macd -> showMacd.postValue(checked)
            ChartIndicator.Rsi -> showRsi.postValue(checked)
        }
        macdIsEnabled = indicator == ChartIndicator.Macd && checked
    }

    //chart indicators should be disabled for daily and 24hrs periods
    private fun updateChartIndicatorState() {
        val enabled = service.chartType != ChartType.DAILY && service.chartType != ChartType.TODAY

        if (setChartIndicatorsEnabled.value == enabled){
            return
        }

        setChartIndicatorsEnabled.postValue(enabled)

        when (enabledIndicator) {
            ChartIndicator.Ema -> showEma.postValue(enabled)
            ChartIndicator.Macd -> showMacd.postValue(enabled)
            ChartIndicator.Rsi -> showRsi.postValue(enabled)
            else -> {
            }
        }
    }

    private fun onChartError(error: Throwable?) {
        showChartError.postValue(Unit)
    }

    private fun fetchChartInfo() {
        chartSpinner.postValue(true)
        service.updateChartInfo()
    }

    private fun syncCoinDetailsState(state: CoinService.CoinDetailsState) {
        marketSpinner.postValue(state is CoinService.CoinDetailsState.Loading)
        if (state is CoinService.CoinDetailsState.Loaded) {
            updateCoinDetails()
        }
    }

    private fun updateAlertNotificationIconState() {
        notificationIconActive = service.hasPriceAlert
        alertNotificationUpdated.postValue(Unit)
    }

    private fun updateFavoriteNotificationItemState() {
        isFavorite.postValue(service.isCoinFavorite())
    }

    private fun updateCoinDetails() {
        val coinDetails = service.coinMarketDetails ?: return
        coinDetailsLiveData.postValue(factory.createCoinDetailsViewItem(coinDetails, service.currency, coinCode, service.guideUrl))

        val coinMarketItems = factory.createCoinMarketItems(coinDetails.tickers)
        val coinInvestorItems = factory.createCoinInvestorItems(coinDetails.meta.fundCategories)
        setExtraPageButtons(coinMarketItems, coinInvestorItems)

        coinMarkets.postValue(coinMarketItems)
        coinInvestors.postValue(coinInvestorItems)
    }

    private fun setExtraPageButtons(coinMarketItems: List<MarketTickerViewItem>, coinInvestorItems: List<InvestorItem>) {
        val coinExtraPages = mutableListOf<CoinExtraPage>()
        if (coinMarketItems.isNotEmpty()) {
            val listPosition = if (coinInvestorItems.isEmpty()) ListPosition.Single else ListPosition.First
            coinExtraPages.add(CoinExtraPage.Markets(listPosition))
        }
        if (coinInvestorItems.isNotEmpty()) {
            val listPosition = if (coinMarketItems.isEmpty()) ListPosition.Single else ListPosition.Last
            coinExtraPages.add(CoinExtraPage.Investors(listPosition))
        }
        extraPages.postValue(coinExtraPages)
    }

    private fun updateChartInfo() {
        val info = service.chartInfo ?: return
        chartSpinner.postValue(false)
        showChartInfo.postValue(factory.createChartInfo(service.chartType, info, service.lastPoint))
    }

    //  ViewModel

    override fun onCleared() {
        disposable.clear()
        clearables.forEach(Clearable::clear)
    }
}
