package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.coin.CoinViewFactory
import io.horizontalsystems.bankwallet.modules.coin.adapters.CoinChartAdapter
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.marketkit.models.ChartType
import io.reactivex.disposables.CompositeDisposable

class CoinOverviewViewModel(
    private val service: CoinOverviewService,
    private val factory: CoinViewFactory,
) : ViewModel() {

    val fullCoin by service::fullCoin
    val currency by service::currency
    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val overviewLiveData = MutableLiveData<CoinOverviewViewItem>()
    val viewStateLiveData = MutableLiveData<ViewState>()
    val chartInfoLiveData = MutableLiveData<CoinChartAdapter.ViewItemWrapper>()
    val titleLiveData = MutableLiveData<String>()

    private val disposables = CompositeDisposable()

    init {
        service.coinOverviewObservable
            .subscribeIO { coinOverview ->
                isRefreshingLiveData.postValue(coinOverview == DataState.Loading)

                coinOverview.dataOrNull?.let {
                    overviewLiveData.postValue(factory.getOverviewViewItem(it, service.fullCoin))
                }

                coinOverview.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
            .let {
                disposables.add(it)
            }

        service.coinPriceObservable
            .subscribeIO { coinPrice ->
                coinPrice.dataOrNull?.let {
                    val currencyValue = CurrencyValue(service.currency, it.value)
                    titleLiveData.postValue(factory.getFormattedLatestRate(currencyValue))
                }
            }
            .let {
                disposables.add(it)
            }

        service.chartDataObservable
            .subscribeIO { chartData ->
                val chartInfoData = chartData.dataOrNull?.let { (chartInfo, lastPoint, chartType) ->
                    factory.createChartInfoData(chartType, chartInfo, lastPoint)
                }

                val chartInfoViewItemWrapper = CoinChartAdapter.ViewItemWrapper(
                    chartInfoData,
                    chartData == DataState.Loading,
                    chartData is DataState.Error
                )

                chartInfoLiveData.postValue(chartInfoViewItemWrapper)
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    fun changeChartType(chartType: ChartView.ChartType) {
        service.changeChartType(getKitChartType(chartType))
    }

    private fun getKitChartType(type: ChartView.ChartType) = when (type) {
        ChartView.ChartType.TODAY -> ChartType.TODAY
        ChartView.ChartType.DAILY -> ChartType.DAILY
        ChartView.ChartType.WEEKLY -> ChartType.WEEKLY
        ChartView.ChartType.WEEKLY2 -> ChartType.WEEKLY2
        ChartView.ChartType.MONTHLY -> ChartType.MONTHLY
        ChartView.ChartType.MONTHLY_BY_DAY -> ChartType.MONTHLY_BY_DAY
        ChartView.ChartType.MONTHLY3 -> ChartType.MONTHLY3
        ChartView.ChartType.MONTHLY6 -> ChartType.MONTHLY6
        ChartView.ChartType.MONTHLY12 -> ChartType.MONTHLY12
        ChartView.ChartType.MONTHLY24 -> ChartType.MONTHLY24
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    fun retry() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }
}
