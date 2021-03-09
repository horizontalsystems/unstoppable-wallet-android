package io.horizontalsystems.bankwallet.modules.coin

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.xrateskit.entities.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class CoinInteractor(
        private val xRateManager: IRateManager,
        private val chartTypeStorage: IChartTypeStorage,
        private val priceAlertManager: IPriceAlertManager,
        private val notificationManager: INotificationManager,
        private val localStorage: ILocalStorage,
        private val marketFavoritesManager: MarketFavoritesManager)
    : CoinModule.Interactor {

    var delegate: CoinModule.InteractorDelegate? = null

    private val disposables = CompositeDisposable()
    private var alertNotificationDisposable: Disposable? = null

    override val notificationsAreEnabled: Boolean
        get() = notificationManager.isEnabled && localStorage.isAlertNotificationOn

    override var defaultChartType: ChartType?
        get() = chartTypeStorage.chartType
        set(value) {
            chartTypeStorage.chartType = value
        }

    override fun getMarketInfo(coinType: CoinType, currencyCode: String): MarketInfo? {
        return xRateManager.marketInfo(coinType, currencyCode)
    }

    override fun getChartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType): ChartInfo? {
        return xRateManager.chartInfo(coinType, currencyCode, chartType)
    }

    override fun getCoinDetails(coinType: CoinType, currencyCode: String, rateDiffCoinCodes: List<String>, rateDiffPeriods: List<TimePeriod>) {
        xRateManager.coinMarketDetailsAsync(coinType, currencyCode, rateDiffCoinCodes, rateDiffPeriods)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ coinMarketDetails ->
                    delegate?.onUpdate(coinMarketDetails)
                }, {
                    delegate?.onMarketError(it)
                }).let {
                    disposables.add(it)
                }
    }

    override fun observeChartInfo(coinType: CoinType, currencyCode: String, chartType: ChartType) {
        xRateManager.chartInfoObservable(coinType, currencyCode, chartType)
                .delay(600, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ chartInfo ->
                    delegate?.onUpdate(chartInfo)
                }, {
                    delegate?.onChartError(it)
                }).let {
                    disposables.add(it)
                }
    }

    override fun observeMarketInfo(coinType: CoinType, currencyCode: String) {
        xRateManager.marketInfoObservable(coinType, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ marketInfo ->
                    delegate?.onUpdate(marketInfo)
                }, {
                    delegate?.onMarketError(it)
                }).let {
                    disposables.add(it)
                }
    }

    override fun observeAlertNotification() {
        alertNotificationDisposable = priceAlertManager.notificationChangedFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    delegate?.updateAlertNotificationIconState()
                }
    }

    override fun getPriceAlert(coinCode: String): PriceAlert {
        return priceAlertManager.getPriceAlert(coinCode)
    }

    override fun isCoinFavorite(coinType: CoinType): Boolean {
        return marketFavoritesManager.isCoinInFavorites(coinType)
    }

    override fun favorite(coinType: CoinType) {
        marketFavoritesManager.add(coinType)

        delegate?.updateFavoriteNotificationItemState()
    }

    override fun unfavorite(coinType: CoinType) {
        marketFavoritesManager.remove(coinType)

        delegate?.updateFavoriteNotificationItemState()
    }

    override fun clear() {
        disposables.clear()
        alertNotificationDisposable?.dispose()
    }
}
