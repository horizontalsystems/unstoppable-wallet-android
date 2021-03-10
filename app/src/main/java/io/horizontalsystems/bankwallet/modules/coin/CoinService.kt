package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class CoinService(
        private val coinType: CoinType,
        val currency: Currency,
        private val xRateManager: IRateManager,
        private val chartTypeStorage: IChartTypeStorage,
        private val priceAlertManager: IPriceAlertManager,
        private val notificationManager: INotificationManager,
        private val localStorage: ILocalStorage,
        private val marketFavoritesManager: MarketFavoritesManager
) {

    sealed class CoinDetailsState {
        object Loading : CoinDetailsState()
        data class Loaded(val coinDetails: CoinMarketDetails) : CoinDetailsState()
        data class Error(val error: Throwable) : CoinDetailsState()
    }

    val chartInfoUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.createDefault(Unit)
    val chartInfoErrorObservable: BehaviorSubject<Throwable> = BehaviorSubject.create()
    val coinDetailsStateObservable: BehaviorSubject<CoinDetailsState> = BehaviorSubject.createDefault(CoinDetailsState.Loading)
    val alertNotificationUpdatedObservable: BehaviorSubject<Unit> = BehaviorSubject.createDefault(Unit)

    var lastPoint: LastPoint? = xRateManager.marketInfo(coinType, currency.code)?.let{ LastPoint(it.rate, it.timestamp) }
        set(value) {
            field = value
            chartInfoUpdatedObservable.onNext(Unit)
        }

    var chartInfo: ChartInfo? = null
        set(value) {
            field = value
            chartInfoUpdatedObservable.onNext(Unit)
        }

    private val disposables = CompositeDisposable()
    private var alertNotificationDisposable: Disposable? = null

    init {
        priceAlertManager.notificationChangedFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    alertNotificationUpdatedObservable.onNext(Unit)
                }
                .let {
                    disposables.add(it)
                }
    }

    val notificationsAreEnabled: Boolean
        get() = notificationManager.isEnabled && localStorage.isAlertNotificationOn

    var chartType: ChartType
        get() = chartTypeStorage.chartType ?: ChartType.TODAY
        set(value) {
            chartTypeStorage.chartType = value
        }

    fun getCoinDetails(rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>) {
        coinDetailsStateObservable.onNext(CoinDetailsState.Loading)
        xRateManager.coinMarketDetailsAsync(coinType, currency.code, rateDiffCoinCodes, rateDiffPeriods)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ coinMarketDetails ->
                    coinDetailsStateObservable.onNext(CoinDetailsState.Loaded(coinMarketDetails))
                }, {
                    coinDetailsStateObservable.onNext(CoinDetailsState.Error(it))
                }).let {
                    disposables.add(it)
                }
    }

    fun updateChartInfo() {
        chartInfo = xRateManager.chartInfo(coinType, currency.code, chartType)
        xRateManager.chartInfoObservable(coinType, currency.code, chartType)
                .delay(600, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ chartInfo ->
                    this.chartInfo = chartInfo
                }, {
                    chartInfoErrorObservable.onNext(it)
                }).let {
                    disposables.add(it)
                }
    }

    fun observeLastPointData() {
        xRateManager.marketInfoObservable(coinType, currency.code)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ marketInfo ->
                    lastPoint = LastPoint(marketInfo.rate, marketInfo.timestamp)
                }, {
                    //ignore
                }).let {
                    disposables.add(it)
                }
    }

    fun getPriceAlert(coinCode: String): PriceAlert {
        return priceAlertManager.getPriceAlert(coinCode)
    }

    fun isCoinFavorite(): Boolean {
        return marketFavoritesManager.isCoinInFavorites(coinType)
    }

    fun favorite() {
        marketFavoritesManager.add(coinType)
    }

    fun unfavorite() {
        marketFavoritesManager.remove(coinType)
    }

    fun clear() {
        disposables.clear()
        alertNotificationDisposable?.dispose()
    }
}
