package cash.p.terminal.modules.calculator.pinsettings

import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.modules.calculator.domain.CalculatorAutoLockOption
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.launch

class CalculatorPinSettingsViewModel(
    private val localStorage: ILocalStorage,
) : ViewModelUiState<CalculatorPinSettingsUiState>() {

    init {
        viewModelScope.launch {
            localStorage.isCalculatorModeEnabledFlow.collect { emitState() }
        }
        viewModelScope.launch {
            localStorage.calculatorAutoLockOptionFlow.collect { emitState() }
        }
    }

    override fun createState() = CalculatorPinSettingsUiState(
        isEnabled = localStorage.isCalculatorModeEnabled,
        autoLockOption = localStorage.calculatorAutoLockOption,
        pushNotificationsEnabled = localStorage.pushNotificationsEnabled,
    )
}

data class CalculatorPinSettingsUiState(
    val isEnabled: Boolean,
    val autoLockOption: CalculatorAutoLockOption,
    val pushNotificationsEnabled: Boolean,
)
