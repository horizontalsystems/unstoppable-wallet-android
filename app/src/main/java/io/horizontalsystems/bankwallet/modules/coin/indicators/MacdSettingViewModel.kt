package io.horizontalsystems.bankwallet.modules.coin.indicators

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ViewModelUiState
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorManager
import io.horizontalsystems.bankwallet.modules.chart.ChartIndicatorSetting

class MacdSettingViewModel(
    private var indicatorSetting: ChartIndicatorSetting,
    private val chartIndicatorManager: ChartIndicatorManager
) : ViewModelUiState<MacdSettingUiState>() {
    val name = indicatorSetting.name
    val defaultFast = indicatorSetting.defaultData["fast"]
    val defaultSlow = indicatorSetting.defaultData["slow"]
    val defaultSignal = indicatorSetting.defaultData["signal"]
    private var fast = indicatorSetting.extraData["fast"]
    private var slow = indicatorSetting.extraData["slow"]
    private var signal = indicatorSetting.extraData["signal"]
    private var fastError: Throwable? = null
    private var slowError: Throwable? = null
    private var signalError: Throwable? = null
    private var finish = false

    override fun createState() = MacdSettingUiState(
        fast = fast,
        slow = slow,
        signal = signal,
        fastError = fastError,
        slowError = slowError,
        signalError = signalError,
        applyEnabled = applyEnabled(),
        finish = finish,
        resetEnabled = resetEnabled()
    )

    private fun applyEnabled(): Boolean {
        if (fastError != null || slowError != null || signalError != null)
            return false

        return fast != indicatorSetting.extraData["fast"] ||
            slow != indicatorSetting.extraData["slow"] ||
            signal != indicatorSetting.extraData["signal"]
    }

    private fun resetEnabled(): Boolean {
        return fast != null || slow != null || signal != null
    }

    fun onEnterFast(v: String) {
        fast = v
        fastError = validateNumber(v)

        emitState()
    }

    fun onEnterSlow(v: String) {
        slow = v
        slowError = validateNumber(v)

        emitState()
    }

    fun onEnterSignal(v: String) {
        signal = v
        signalError = validateNumber(v)

        emitState()
    }

    private fun validateNumber(v: String): Throwable? {
        if (v.isBlank()) {
            return null
        }

        val number = v.toIntOrNull() ?: return NotIntegerException()
        if (number < 2 || number > 200) {
            return OutOfRangeException(2, 200)
        }
        return null
    }

    fun save() {
        val extraData = indicatorSetting.extraData.plus(
            mapOf(
                "fast" to fast,
                "slow" to slow,
                "signal" to signal,
            )
        )
        val updated = indicatorSetting.copy(extraData = extraData)
        chartIndicatorManager.update(updated)

        finish = true
        emitState()
    }

    fun reset() {
        fast = null
        slow = null
        signal = null

        emitState()
    }

    class Factory(private val indicatorSetting: ChartIndicatorSetting) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MacdSettingViewModel(indicatorSetting, App.chartIndicatorManager) as T
        }
    }
}

data class MacdSettingUiState(
    val fast: String?,
    val slow: String?,
    val signal: String?,
    val fastError: Throwable?,
    val slowError: Throwable?,
    val signalError: Throwable?,
    val applyEnabled: Boolean,
    val finish: Boolean,
    val resetEnabled: Boolean,
)
