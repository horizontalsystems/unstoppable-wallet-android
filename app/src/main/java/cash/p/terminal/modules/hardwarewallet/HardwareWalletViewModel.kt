package cash.p.terminal.modules.hardwarewallet

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.core.App
import cash.p.terminal.core.IAccountFactory
import cash.p.terminal.core.usecase.CreateHardwareWalletUseCase
import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.usecase.TangemScanUseCase
import cash.p.terminal.tangem.ui.HardwareWalletError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class HardwareWalletViewModel(
    private val tangemScanUseCase: TangemScanUseCase,
    private val createHardwareWalletUseCase: CreateHardwareWalletUseCase
) : ViewModel() {
    private val accountFactory: IAccountFactory = App.accountFactory

    val defaultAccountName = accountFactory.getNextHardwareAccountName()
    var accountName: String = defaultAccountName
        get() = field.ifBlank { defaultAccountName }
        private set

    var success by mutableStateOf<Boolean>(false)

    private val _errorEvents = Channel<HardwareWalletError>(capacity = 1)
    val errorEvents = _errorEvents.receiveAsFlow()

    fun scanCard() = viewModelScope.launch {
        tangemScanUseCase.scanProduct(TangemConfig.getDefaultTokens)
            .doOnSuccess { scanResponse ->
                if (scanResponse.card.isAccessCodeSet && scanResponse.card.wallets.isNotEmpty()) {
                    // Card already activated, need to add tokens
                    createHardwareWalletUseCase(accountName, scanResponse)
                    success = true
                } else {
                    _errorEvents.trySend(HardwareWalletError.CardNotActivated)
                }
            }
            .doOnFailure {
                Log.d("CreateAccountViewModel", "Error scanning card: ${it.customMessage}")
            }
    }

    fun onChangeAccountName(name: String) {
        accountName = name
    }
}
