package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.core.managers.MarketFavoritesManager
import io.horizontalsystems.bankwallet.entities.PriceAlert
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateChartInteractor(
        private val xRateManager: IRateManager,
        private val chartTypeStorage: IChartTypeStorage,
        private val priceAlertManager: IPriceAlertManager,
        private val notificationManager: INotificationManager,
        private val localStorage: ILocalStorage,
        private val marketFavoritesManager: MarketFavoritesManager)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private var mInfoDisposable: Disposable? = null
    private var cInfoDisposable: Disposable? = null
    private var alertNotificationDisposable: Disposable? = null

    override val notificationsAreEnabled: Boolean
        get() = notificationManager.isEnabled && localStorage.isAlertNotificationOn

    override var defaultChartType: ChartType?
        get() = chartTypeStorage.chartType
        set(value) {
            chartTypeStorage.chartType = value
        }

    override fun getMarketInfo(coinCode: String, currencyCode: String): MarketInfo? {
        return xRateManager.marketInfo(coinCode, currencyCode)
    }

    override fun getChartInfo(coinCode: String, currencyCode: String, chartType: ChartType): ChartInfo? {
        return xRateManager.chartInfo(coinCode, currencyCode, chartType)
    }

    override fun observeChartInfo(coinCode: String, currencyCode: String, chartType: ChartType) {
        cInfoDisposable?.dispose()
        cInfoDisposable = xRateManager.chartInfoObservable(coinCode, currencyCode, chartType)
                .delay(600, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ chartInfo ->
                    delegate?.onUpdate(chartInfo)
                }, {
                    delegate?.onError(it)
                })
    }

    override fun observeMarketInfo(coinCode: String, currencyCode: String) {
        mInfoDisposable?.dispose()
        mInfoDisposable = xRateManager.marketInfoObservable(coinCode, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ marketInfo ->
                    delegate?.onUpdate(marketInfo)
                }, {
                    delegate?.onError(it)
                })
    }

    override fun observeAlertNotification(coinCode: String) {
        alertNotificationDisposable = priceAlertManager.notificationChangedFlowable
                .subscribeOn(Schedulers.io())
                .subscribe {
                    delegate?.updateAlertNotificationIconState()
                }
    }

    override fun getPriceAlert(coinCode: String): PriceAlert {
        return priceAlertManager.getPriceAlert(coinCode)
    }

    override fun isCoinFavorite(coinCode: String): Boolean {
        return marketFavoritesManager.isCoinInFavorites(coinCode)
    }

    override fun favorite(coinCode: String) {
        marketFavoritesManager.add(coinCode)

        delegate?.updateFavoriteNotificationItemState()
    }

    override fun unfavorite(coinCode: String) {
        marketFavoritesManager.remove(coinCode)

        delegate?.updateFavoriteNotificationItemState()
    }

    override fun clear() {
        mInfoDisposable?.dispose()
        cInfoDisposable?.dispose()
        alertNotificationDisposable?.dispose()
    }
}
