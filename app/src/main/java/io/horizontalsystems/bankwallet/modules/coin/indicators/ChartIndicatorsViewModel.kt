package cash.p.terminal.modules.coin.indicators

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartIndicator
import cash.p.terminal.modules.chart.ChartIndicatorManager
import io.horizontalsystems.chartview.models.ChartIndicatorType
import kotlinx.coroutines.launch

class ChartIndicatorsViewModel(
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModel() {
    private var maIndicators: List<ChartIndicator> = listOf()
    private var oscillatorIndicators: List<ChartIndicator> = listOf()

    var uiState by mutableStateOf(
        ChartIndicatorsUiState(
            maIndicators = maIndicators,
            oscillatorIndicators = oscillatorIndicators
        )
    )
        private set

    init {
        viewModelScope.launch {
            chartIndicatorManager.allIndicatorsFlow.collect {
                maIndicators = it.filter { it.indicatorType is ChartIndicatorType.MovingAverage }
                oscillatorIndicators = it.filter { it.indicatorType is ChartIndicatorType.Rsi || it.indicatorType is ChartIndicatorType.Macd }

                emitState()
            }
        }
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = ChartIndicatorsUiState(
                maIndicators = maIndicators,
                oscillatorIndicators = oscillatorIndicators
            )
        }
    }

    fun enable(indicatorId: String) {
        chartIndicatorManager.enableIndicator(indicatorId)
    }

    fun disable(indicatorId: String) {
        chartIndicatorManager.disableIndicator(indicatorId)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChartIndicatorsViewModel(App.chartIndicatorManager) as T
        }
    }

}


data class ChartIndicatorsUiState(
    val maIndicators: List<ChartIndicator>,
    val oscillatorIndicators: List<ChartIndicator>
)
