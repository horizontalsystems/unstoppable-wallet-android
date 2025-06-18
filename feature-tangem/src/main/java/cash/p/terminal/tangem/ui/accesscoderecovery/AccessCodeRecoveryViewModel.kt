package cash.p.terminal.tangem.ui.accesscoderecovery

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.common.doOnSuccess
import kotlinx.coroutines.launch

internal class AccessCodeRecoveryViewModel(
    private val tangemSdkManager: TangemSdkManager
) : ViewModel() {

    var enabledDefaultState: Boolean = false

    private val _enabled = mutableStateOf(enabledDefaultState)
    val enabled: State<Boolean> get() = _enabled

    private val _success = mutableStateOf(false)
    val success: State<Boolean> get() = _success

    fun setEnabled(value: Boolean) {
        _enabled.value = value
    }

    fun saveChanges() = viewModelScope.launch {
        tangemSdkManager.setAccessCodeRecoveryEnabled(cardId = null, enabled = _enabled.value)
            .doOnSuccess {
                _success.value = true
            }
    }
}