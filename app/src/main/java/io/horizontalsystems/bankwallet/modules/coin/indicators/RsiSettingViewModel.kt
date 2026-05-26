package io.horizontalsystems.bankwallet.modules.coin.indicators

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = RsiSettingViewModel.Factory::class)
class RsiSettingViewModel @AssistedInject constructor(
    @Assisted private var indicatorSetting: ChartIndicatorSetting,
    private val chartIndicatorManager: ChartIndicatorManager,
) : ViewModelUiState<IndicatorSettingUiState>() {

    @AssistedFactory
    interface Factory {
        fun create(indicatorSetting: ChartIndicatorSetting): RsiSettingViewModel
    }

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
}

data class IndicatorSettingUiState(
    val period: String?,
    val periodError: Throwable?,
    val applyEnabled: Boolean,
    val finish: Boolean,
    val resetEnabled: Boolean,
)
