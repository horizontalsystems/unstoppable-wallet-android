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

class RsiSettingViewModel(
    private var indicatorSetting: ChartIndicatorSetting,
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModel() {
    val name = indicatorSetting.name
    val defaultPeriod = indicatorSetting.defaultData["period"]
    private var period = indicatorSetting.extraData["period"]
    private var periodError: Throwable? = null
    private var finish = false

    var uiState by mutableStateOf(
        IndicatorSettingUiState(
            period = period,
            periodError = periodError,
            applyEnabled = applyEnabled(),
            finish = finish,
            resetEnabled = resetEnabled()
        )
    )
        private set

    private fun applyEnabled(): Boolean {
        return period != indicatorSetting.extraData["period"]
    }

    private fun resetEnabled(): Boolean {
        return period != null
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = IndicatorSettingUiState(
                period = period,
                periodError = periodError,
                applyEnabled = applyEnabled(),
                finish = finish,
                resetEnabled = resetEnabled(),
            )
        }
    }

    fun onEnterPeriod(v: String) {
        period = v

        periodError = null
        if (v.isNotBlank()) {
            val number = v.toIntOrNull()
            if (number == null) {
                periodError = NotIntegerException()
            } else if (number < 2 || number > 200) {
                periodError = OutOfRangeException(2, 200)
            }
        }

        emitState()
    }

    fun save() {
        val extraData = indicatorSetting.extraData.plus(
            mapOf(
                "period" to period,
            )
        )
        val updated = indicatorSetting.copy(extraData = extraData)
        chartIndicatorManager.update(updated)

        finish = true
        emitState()
    }

    fun reset() {
        period = null

        emitState()
    }

    class Factory(private val indicatorSetting: ChartIndicatorSetting) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RsiSettingViewModel(indicatorSetting, App.chartIndicatorManager) as T
        }
    }
}

data class IndicatorSettingUiState(
    val period: String?,
    val periodError: Throwable?,
    val applyEnabled: Boolean,
    val finish: Boolean,
    val resetEnabled: Boolean,
)
