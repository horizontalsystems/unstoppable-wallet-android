package io.horizontalsystems.bankwallet.modules.coin

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.chartview.models.PointInfo
import io.horizontalsystems.core.SingleLiveEvent
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.disposables.CompositeDisposable

class CoinViewModel(
        val rateFormatter: RateFormatter,
        private val service: CoinService,
        private val coinCode: String,
        private val coinTitle: String,
        private val coinId: String?,
        private val factory: RateChartViewFactory)
    : ViewModel() {

    val chartSpinner = SingleLiveEvent<Boolean>()
    val marketSpinner = SingleLiveEvent<Boolean>()
    val setDefaultMode = SingleLiveEvent<ChartType>()
    val setSelectedPoint = SingleLiveEvent<ChartPointViewItem>()
    val showChartInfo = SingleLiveEvent<ChartInfoViewItem>()
    val coinDetailsLiveData = MutableLiveData<CoinDetailsViewItem>()
    val showChartError = SingleLiveEvent<Unit>()
    val showEma = SingleLiveEvent<Boolean>()
    val showMacd = SingleLiveEvent<Boolean>()
    val showRsi = SingleLiveEvent<Boolean>()
    val alertNotificationUpdated = SingleLiveEvent<Unit>()
    val showNotificationMenu = SingleLiveEvent<Pair<String, String>>()
    val isFavorite = MutableLiveData<Boolean>()

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

    private fun onChartError(error: Throwable?) {
        showChartError.call()
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

    private fun fetchChartInfo() {
        chartSpinner.postValue(true)
        service.observeLastPointData()
        service.updateChartInfo()
    }

    private fun syncCoinDetailsState(state: CoinService.CoinDetailsState) {
        marketSpinner.postValue(state is CoinService.CoinDetailsState.Loading)
        if (state is CoinService.CoinDetailsState.Loaded) {
            updateCoinDetails(state.coinDetails)
        }
    }

    private fun updateAlertNotificationIconState() {
        val coinId = coinId ?: return
        val priceAlert = service.getPriceAlert(coinId)
        notificationIconActive = priceAlert.changeState != PriceAlert.ChangeState.OFF || priceAlert.trendState != PriceAlert.TrendState.OFF
        alertNotificationUpdated.call()
    }

    private fun updateFavoriteNotificationItemState() {
        isFavorite.postValue(service.isCoinFavorite())
    }

    private fun updateCoinDetails(coinDetails: CoinMarketDetails) {
        coinDetailsLiveData.postValue(factory.createCoinDetailsViewItem(coinDetails, service.currency, coinCode))
    }

    private fun updateChartInfo() {
        val info = service.chartInfo ?: return
        chartSpinner.postValue(false)
        showChartInfo.postValue(factory.createChartInfo(service.chartType, info, service.lastPoint))
    }

    //  ViewModel

    override fun onCleared() {
        service.clear()
        disposable.clear()
    }
}
