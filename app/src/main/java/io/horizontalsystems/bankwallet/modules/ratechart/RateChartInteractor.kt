package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

class RateChartInteractor(private val xRateManager: IRateManager, private val localStorage: IChartTypeStorage)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private var mInfoDisposable: Disposable? = null
    private var cInfoDisposable: Disposable? = null

    override var defaultChartType: ChartType?
        get() = localStorage.chartType
        set(value) {
            localStorage.chartType = value
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

    override fun clear() {
        mInfoDisposable?.dispose()
        cInfoDisposable?.dispose()
    }
}
