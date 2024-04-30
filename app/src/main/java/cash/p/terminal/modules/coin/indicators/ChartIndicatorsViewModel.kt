package cash.p.terminal.modules.coin.indicators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.ViewModelUiState
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import kotlinx.coroutines.launch

class ChartIndicatorsViewModel(
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModelUiState<ChartIndicatorsUiState>() {
    private var maIndicators: List<ChartIndicatorSetting> = listOf()
    private var oscillatorIndicators: List<ChartIndicatorSetting> = listOf()

    init {
        viewModelScope.launch {
            chartIndicatorManager.allIndicatorsFlow.collect {
                maIndicators = it.filter { it.type == ChartIndicatorSetting.IndicatorType.MA }
                oscillatorIndicators = it.filter { it.type == ChartIndicatorSetting.IndicatorType.RSI || it.type == ChartIndicatorSetting.IndicatorType.MACD }

                emitState()
            }
        }
    }

    override fun createState() = ChartIndicatorsUiState(
        maIndicators = maIndicators,
        oscillatorIndicators = oscillatorIndicators
    )

    fun enable(indicator: ChartIndicatorSetting) {
        chartIndicatorManager.enableIndicator(indicator.id)

        if (oscillatorIndicators.contains(indicator)) {
            oscillatorIndicators.minus(indicator).forEach {
                chartIndicatorManager.disableIndicator(it.id)
            }
        }
    }

    fun disable(indicator: ChartIndicatorSetting) {
        chartIndicatorManager.disableIndicator(indicator.id)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChartIndicatorsViewModel(App.chartIndicatorManager) as T
        }
    }

}


data class ChartIndicatorsUiState(
    val maIndicators: List<ChartIndicatorSetting>,
    val oscillatorIndicators: List<ChartIndicatorSetting>
)
