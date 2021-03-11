package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.disposables.CompositeDisposable

class CoinViewModel(
        val rateFormatter: RateFormatter,
        private val service: CoinService,
        val coinCode: String,
        private val coinTitle: String,
        private val coinId: String?,
        private val factory: RateChartViewFactory,
        private val clearables: List<Clearable>
        )
    : ViewModel() {

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
    val alertNotificationUpdated = MutableLiveData<Unit>()
    val showNotificationMenu = MutableLiveData<Pair<String, String>>()
    val isFavorite = MutableLiveData<Boolean>()
    val coinMarkets = MutableLiveData<List<MarketTickerViewItem>>()

    var notificationIconVisible = coinId != null && service.notificationsAreEnabled
    var notificationIconActive = false

    private var emaIsEnabled = false
    private var macdIsEnabled = false
    private var rsiIsEnabled = false
    private val disposable = CompositeDisposable()


    init {
        setDefaultMode.postValue(service.chartType)

        updateChartInfo()

        service.getCoinDetails(listOf("USD", "BTC", "ETH"), listOf(TimePeriod.DAY_7, TimePeriod.DAY_30))

        fetchChartInfo()

        updateAlertNotificationIconState()

        updateFavoriteNotificationItemState()

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

    fun onSelect(type: ChartType) {
        if (service.chartType == type)
            return

        service.chartType = type

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
        coinId?.let {
            showNotificationMenu.postValue(Pair(it, coinTitle))
        }
    }

    fun onFavoriteClick() {
        service.favorite()
        updateFavoriteNotificationItemState()
    }

    fun onUnfavoriteClick() {
        service.unfavorite()
        updateFavoriteNotificationItemState()
    }

    fun toggleEma() {
        emaIsEnabled = !emaIsEnabled
        showEma.postValue(emaIsEnabled)
    }

    fun toggleMacd() {
        if (rsiIsEnabled) {
            toggleRsi()
        }

        macdIsEnabled = !macdIsEnabled
        showMacd.postValue(macdIsEnabled)
    }

    fun toggleRsi() {
        if (macdIsEnabled) {
            toggleMacd()
        }

        rsiIsEnabled = !rsiIsEnabled
        showRsi.postValue(rsiIsEnabled)
    }

    private fun onChartError(error: Throwable?) {
        showChartError.postValue(Unit)
    }

    private fun fetchChartInfo() {
        chartSpinner.postValue(true)
        service.observeLastPointData()
        service.updateChartInfo()
    }

    private fun syncCoinDetailsState(state: CoinService.CoinDetailsState) {
        marketSpinner.postValue(state is CoinService.CoinDetailsState.Loading)
        if (state is CoinService.CoinDetailsState.Loaded) {
            updateCoinDetails()
        }
    }

    private fun updateAlertNotificationIconState() {
        val coinId = coinId ?: return
        val priceAlert = service.getPriceAlert(coinId)
        notificationIconActive = priceAlert.changeState != PriceAlert.ChangeState.OFF || priceAlert.trendState != PriceAlert.TrendState.OFF
        alertNotificationUpdated.postValue(Unit)
    }

    private fun updateFavoriteNotificationItemState() {
        isFavorite.postValue(service.isCoinFavorite())
    }

    private fun updateCoinDetails() {
        val coinDetails = service.coinMarketDetails ?: return
        coinDetailsLiveData.postValue(factory.createCoinDetailsViewItem(coinDetails, service.currency, coinCode))
        coinMarkets.postValue(factory.createCoinMarketItems(coinDetails))
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
