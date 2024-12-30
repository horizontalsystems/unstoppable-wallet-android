package cash.p.terminal.modules.coin.indicators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.core.ViewModelUiState
import cash.p.terminal.modules.chart.ChartIndicatorManager
import cash.p.terminal.modules.chart.ChartIndicatorSetting

class RsiSettingViewModel(
    private var indicatorSetting: ChartIndicatorSetting,
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModelUiState<IndicatorSettingUiState>() {
    val name = indicatorSetting.name
    val defaultPeriod = indicatorSetting.defaultData["period"]
    private var period = indicatorSetting.extraData["period"]
    private var periodError: Throwable? = null
    private var finish = false

    override fun createState() = IndicatorSettingUiState(
        period = period,
        periodError = periodError,
        applyEnabled = applyEnabled(),
        finish = finish,
        resetEnabled = resetEnabled()
    )

    private fun applyEnabled(): Boolean {
        return period != indicatorSetting.extraData["period"]
    }

    private fun resetEnabled(): Boolean {
        return period != null
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
