package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartIndicatorsViewModel @Inject constructor(
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

}


data class ChartIndicatorsUiState(
    val maIndicators: List<ChartIndicatorSetting>,
    val oscillatorIndicators: List<ChartIndicatorSetting>
)
