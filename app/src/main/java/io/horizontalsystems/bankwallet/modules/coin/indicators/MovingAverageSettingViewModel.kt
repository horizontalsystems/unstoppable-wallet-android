package cash.p.terminal.modules.coin.indicators

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.chart.ChartIndicatorSetting
import kotlinx.coroutines.launch

class MovingAverageSettingViewModel(
    private var indicatorSetting: ChartIndicatorSetting,
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModel() {
    val name = indicatorSetting.name
    val maTypes = listOf("EMA", "SMA", "WMA")
    val defaultMaType = indicatorSetting.defaultData["maType"] ?: ""
    val defaultPeriod = indicatorSetting.defaultData["period"]
    private var maType = indicatorSetting.extraData["maType"]
    private var period = indicatorSetting.extraData["period"]
    private var finish = false

    var uiState by mutableStateOf(
        MovingAverageSettingUiState(
            maType = maType,
            period = period,
            applyEnabled = applyEnabled(),
            resetEnabled = resetEnabled(),
            finish = finish
        )
    )
        private set

    private fun applyEnabled(): Boolean {
        return maType != indicatorSetting.extraData["maType"] ||
            period != indicatorSetting.extraData["period"]
    }

    private fun resetEnabled(): Boolean {
        return maType != null || period != null
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = MovingAverageSettingUiState(
                maType = maType,
                period = period,
                applyEnabled = applyEnabled(),
                finish = finish,
                resetEnabled = resetEnabled(),
            )
        }
    }

    fun onSelectMaType(v: String) {
        maType = v
        emitState()
    }

    fun onEnterPeriod(v: String) {
        period = v
        emitState()
    }

    fun save() {
        val extraData = indicatorSetting.extraData.plus(
            mapOf(
                "maType" to maType,
                "period" to period,
            )
        )
        val updated = indicatorSetting.copy(extraData = extraData)
        chartIndicatorManager.update(updated)

        finish = true
        emitState()
    }

    fun reset() {
        maType = null
        period = null

        emitState()
    }

    class Factory(private val indicatorSetting: ChartIndicatorSetting) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MovingAverageSettingViewModel(indicatorSetting, App.chartIndicatorManager) as T
        }
    }
}

data class MovingAverageSettingUiState(
    val maType: String?,
    val period: String?,
    val applyEnabled: Boolean,
    val finish: Boolean,
    val resetEnabled: Boolean,
)
