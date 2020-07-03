package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.IPriceAlertManager
import io.horizontalsystems.bankwallet.core.IRateManager
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
        private val localStorage: ILocalStorage)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private var mInfoDisposable: Disposable? = null
    private var cInfoDisposable: Disposable? = null
    private var alertNotificationDisposable: Disposable? = null

    override var notificationIsOn: Boolean
        get() = localStorage.isAlertNotificationOn
        set(value) {
            localStorage.isAlertNotificationOn = value
        }

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
                    delegate?.alertNotificationsUpdated()
                }
    }

    override fun getPriceAlert(coinCode: String): PriceAlert {
        return priceAlertManager.getPriceAlert(coinCode)
    }

    override fun clear() {
        mInfoDisposable?.dispose()
        cInfoDisposable?.dispose()
        alertNotificationDisposable?.dispose()
    }
}
