package cash.p.terminal.modules.settings.advancedsecurity

import androidx.lifecycle.viewModelScope
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.ViewModelUiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class AdvancedSecurityViewModel(
    private val pinComponent: IPinComponent
) : ViewModelUiState<AdvancedSecurityUiState>() {

    init {
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                emitState()
            }
        }
    }

    override fun createState() = AdvancedSecurityUiState(
        isSecureResetPinSet = pinComponent.isSecureResetPinSet(),
        isDeleteContactsPinSet = pinComponent.isDeleteContactsPinSet()
    )

    fun onSecureResetEnabled() {
        emitState()
    }

    fun onSecureResetDisabled() {
        pinComponent.disableSecureResetPin()
        emitState()
    }

    fun onDeleteContactsDisabled() {
        pinComponent.disableDeleteContactsPin()
        emitState()
    }
}

data class AdvancedSecurityUiState(
    val isSecureResetPinSet: Boolean,
    val isDeleteContactsPinSet: Boolean
)
