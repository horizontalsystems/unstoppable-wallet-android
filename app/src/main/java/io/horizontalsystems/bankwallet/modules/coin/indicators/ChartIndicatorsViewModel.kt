package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import kotlinx.coroutines.launch

class ChartIndicatorsViewModel(
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModel() {
    private var maIndicators: List<ChartIndicatorSetting> = listOf()
    private var oscillatorIndicators: List<ChartIndicatorSetting> = listOf()

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
                maIndicators = it.filter { it.type == ChartIndicatorSetting.IndicatorType.MA }
                oscillatorIndicators = it.filter { it.type == ChartIndicatorSetting.IndicatorType.RSI || it.type == ChartIndicatorSetting.IndicatorType.MACD }

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
