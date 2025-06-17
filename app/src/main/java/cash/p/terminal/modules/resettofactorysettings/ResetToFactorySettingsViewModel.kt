package cash.p.terminal.modules.resettofactorysettings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.usecase.ResetToFactorySettingsUseCase
import cash.p.terminal.tangem.ui.HardwareWalletError
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.IAccountManager
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import kotlin.getValue

internal class ResetToFactorySettingsViewModel(
    private val resetToFactorySettingsUseCase: ResetToFactorySettingsUseCase
) : ViewModel() {

    private val accountManager: IAccountManager by inject(IAccountManager::class.java)

    private val _uiState =
        mutableStateOf(ResetToFactorySettingsViewUIState())
    val uiState: State<ResetToFactorySettingsViewUIState> get() = _uiState

    private val _errorEvents = Channel<HardwareWalletError>(capacity = 1)
    val errorEvents = _errorEvents.receiveAsFlow()

    var account: Account? = null
        set(value) {
            field = value

            val accountType = (value?.type as AccountType.HardwareCard)
            _uiState.value = uiState.value.copy(
                primaryCardId = accountType.cardId,
                backupCards = accountType.backupCardsCount
            )
        }

    // To match backup cards for reset
    private var firstWalletPublicKey: ByteArray? = null

    fun resetCards() {
        viewModelScope.launch {
            if (!uiState.value.primaryCardWasReset) {
                resetToFactorySettingsUseCase.resetPrimaryCard(uiState.value.primaryCardId, false)
                    .doOnSuccess { (walletPublicKey, success) ->
                        firstWalletPublicKey = walletPublicKey
                        if (success) {
                            _uiState.value = uiState.value.copy(
                                primaryCardWasReset = true,
                                currentBackupCard = 1
                            )
                            delay(TangemConfig.SCAN_DELAY)
                            resetCards()
                        }
                    }.doOnFailure {
                        handleTangemError(it)
                    }
            } else if (uiState.value.backupCards > 0 &&
                uiState.value.currentBackupCard <= uiState.value.backupCards
            ) {
                val firstWalletPublicKey = firstWalletPublicKey
                if(firstWalletPublicKey == null) {
                    _errorEvents.trySend(HardwareWalletError.UnknownError)
                    return@launch
                }
                resetToFactorySettingsUseCase.resetBackupCard(
                    cardNumber = uiState.value.currentBackupCard,
                    firstWalletPublicKey = firstWalletPublicKey
                ).doOnSuccess { (_, success) ->
                    if (success) {
                        _uiState.value = uiState.value.copy(
                            currentBackupCard = uiState.value.currentBackupCard + 1
                        )
                        delay(TangemConfig.SCAN_DELAY)
                        resetCards()
                    }
                }.doOnFailure {
                    handleTangemError(it)
                }
            } else {
                // No reset cards left
                _uiState.value = uiState.value.copy(
                    success = true
                )
            }
        }
    }

    fun deleteAccount() {
        accountManager.delete(account!!.id)
    }

    private fun handleTangemError(error: TangemError) {
        if(error !is TangemSdkError.UserCancelled) {
            _errorEvents.trySend(HardwareWalletError.UnknownError)
        }
    }
}

internal data class ResetToFactorySettingsViewUIState(
    val primaryCardWasReset: Boolean = false,
    val primaryCardId: String? = null,
    val currentBackupCard: Int = 0,
    val backupCards: Int = 0,
    val success: Boolean = false
)