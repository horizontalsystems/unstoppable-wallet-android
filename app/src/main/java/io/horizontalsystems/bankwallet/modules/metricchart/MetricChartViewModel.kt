package io.horizontalsystems.bankwallet.modules.metricchart

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.chartview.ChartView.ChartType
import io.horizontalsystems.chartview.models.PointInfo
import io.reactivex.disposables.CompositeDisposable

class MetricChartViewModel(
        private val service: MetricChartService,
        private val chartConfiguration: MetricChartModule.IMetricChartConfiguration,
        private val factory: MetricChartFactory,
        private val clearables: List<MetricChartService>
) : ViewModel() {

    val chartViewItemLiveData = MutableLiveData<ChartViewItem>()
    val selectedPointLiveData = MutableLiveData<SelectedPoint>()
    val toastLiveData = MutableLiveData<String>()
    val loadingLiveData = MutableLiveData<Boolean>()

    var chartType: ChartType = ChartType.DAILY

    val title: Int = chartConfiguration.title

    val description: Int? = chartConfiguration.description

    private val disposables = CompositeDisposable()

    init {
        fetchChartInfo()

        service.stateObservable
                .subscribeIO {
                    sync(it)
                }
                .let {
                    disposables.add(it)
                }
    }

    private fun sync(dataState: DataState<List<MetricChartModule.Item>>) {
        loadingLiveData.postValue(dataState.loading)

        dataState.errorOrNull?.let {
            toastLiveData.postValue(convertErrorMessage(it))
            return
        }

        val data = dataState.dataOrNull ?: return

        val chartViewItem = factory.convert(data, chartType, chartConfiguration.valueType, service.currency)

        chartViewItemLiveData.postValue(chartViewItem)
    }

    fun onChartTypeSelect(chartType: ChartType) {
        this.chartType = chartType
        fetchChartInfo()
    }

    fun onTouchSelect(point: PointInfo) {
        selectedPointLiveData.postValue(factory.selectedPointViewItem(point, chartConfiguration.valueType, service.currency))
    }

    override fun onCleared() {
        clearables.forEach(Clearable::clear)
        disposables.clear()
    }

    private fun fetchChartInfo() {
        service.updateChartInfo(chartType)
    }

    private fun convertErrorMessage(it: Throwable): String {
        return it.message ?: it.javaClass.simpleName
    }

}
