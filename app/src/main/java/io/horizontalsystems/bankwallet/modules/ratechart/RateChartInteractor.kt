package io.horizontalsystems.bankwallet.modules.ratechart

import io.horizontalsystems.bankwallet.core.IChartTypeStorage
import io.horizontalsystems.bankwallet.core.IXRateManager
import io.horizontalsystems.xrateskit.entities.ChartInfo
import io.horizontalsystems.xrateskit.entities.ChartType
import io.horizontalsystems.xrateskit.entities.MarketInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class RateChartInteractor(private val xRateManager: IXRateManager, private val localStorage: IChartTypeStorage)
    : RateChartModule.Interactor {

    var delegate: RateChartModule.InteractorDelegate? = null

    private val disposables = CompositeDisposable()

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
        xRateManager.chartInfoObservable(coinCode, currencyCode, chartType)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ chartInfo ->
                    delegate?.onUpdate(chartInfo)
                }, {
                    delegate?.onError(it)
                })
                .let {
                    disposables.add(it)
                }
    }

    override fun observeMarketInfo(coinCode: String, currencyCode: String) {
        xRateManager.marketInfoObservable(coinCode, currencyCode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ marketInfo ->
                    delegate?.onUpdate(marketInfo)
                }, {
                    delegate?.onError(it)
                })
                .let { disposables.add(it) }
    }

    override fun clear() {
        disposables.clear()
    }
}
