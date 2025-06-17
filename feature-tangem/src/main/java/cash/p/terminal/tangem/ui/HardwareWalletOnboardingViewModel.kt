package cash.p.terminal.tangem.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.tangem.domain.usecase.BackupHardwareWalletUseCase
import cash.p.terminal.tangem.domain.usecase.ICreateHardwareWalletUseCase
import cash.p.terminal.tangem.domain.usecase.ResetToFactorySettingsUseCase
import cash.p.terminal.tangem.domain.usecase.TangemCreateWalletsUseCase
import cash.p.terminal.tangem.domain.usecase.ValidateBackUseCase
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.TangemSdkError
import com.tangem.common.doOnFailure
import com.tangem.common.doOnSuccess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class HardwareWalletOnboardingViewModel(
    private val tangemCreateWalletUseCase: TangemCreateWalletsUseCase,
    private val backupHardwareWalletUseCase: BackupHardwareWalletUseCase,
    private val createHardwareWalletUseCase: ICreateHardwareWalletUseCase,
    private val resetToFactorySettingsUseCase: ResetToFactorySettingsUseCase,
    private val validateBackUseCase: ValidateBackUseCase
) : ViewModel() {

    private companion object {
        const val MAX_BACKUP_CARDS = 2
    }

    private val _uiState =
        mutableStateOf(HardwareWalletOnboardingUIState())
    val uiState: State<HardwareWalletOnboardingUIState> get() = _uiState

    private val _errorEvents = Channel<HardwareWalletError>(capacity = 1)
    val errorEvents = _errorEvents.receiveAsFlow()

    var accountName: String? = null

    init {
        updateActualPage()
    }

    private fun updateActualPage() {
        var card = tangemCreateWalletUseCase.tangemSdkManager.lastScanResponse?.card ?: return
        if (card.wallets.isNotEmpty() &&
            card.backupStatus == Card.BackupStatus.NoBackup
        ) {
            _uiState.value = _uiState.value.copy(
                currentStep = OnboardingStep.ADD_BACKUP
            )
        }
    }

    fun createWallet() = viewModelScope.launch {
        var lastScanResponse = tangemCreateWalletUseCase.tangemSdkManager.lastScanResponse
        if (lastScanResponse != null) {
            tangemCreateWalletUseCase(lastScanResponse, false)
                .doOnSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        currentStep = OnboardingStep.ADD_BACKUP
                    )
                }.doOnFailure {
                    _errorEvents.trySend(HardwareWalletError.WalletsNotCreated)
                }
        } else {
            _errorEvents.trySend(HardwareWalletError.UnknownError)
        }
    }

    fun onGoToFinalPageClick() {
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.CREATE_ACCESS_CODE
        )
    }

    fun createBackup() = viewModelScope.launch {
        var primaryCard = tangemCreateWalletUseCase.tangemSdkManager.lastScanResponse?.primaryCard
        if (primaryCard == null) {
            _uiState.value = _uiState.value.copy(
                currentStep = OnboardingStep.CREATE_WALLET
            )
            return@launch
        }

        backupHardwareWalletUseCase.addBackup(primaryCard)
            .doOnSuccess { card ->
                _uiState.value = _uiState.value.copy(
                    primaryCardId = primaryCard.cardId,
                    backupCards = _uiState.value.backupCards + card
                )
                if (backupHardwareWalletUseCase.addedBackupCardsCount == MAX_BACKUP_CARDS) {
                    _uiState.value = _uiState.value.copy(
                        currentStep = OnboardingStep.CREATE_ACCESS_CODE
                    )
                }
            }
            .doOnFailure { error ->
                when (error) {
                    is TangemSdkError.CardVerificationFailed -> {
                        _errorEvents.trySend(HardwareWalletError.AttestationFailed)
                    }

                    is TangemSdkError.BackupFailedNotEmptyWallets -> {
                        _errorEvents.trySend(HardwareWalletError.NeedFactoryReset(error.cardId))
                    }

                    is TangemSdkError.IssuerSignatureLoadingFailed -> {
                        _errorEvents.trySend(HardwareWalletError.AttestationFailed)
                    }

                    else -> {
                        _errorEvents.trySend(HardwareWalletError.UnknownError)
                    }
                }
            }
    }

    fun setAccessCode(accessCode: String) {
        backupHardwareWalletUseCase.setAccessCode(accessCode)
        _uiState.value = _uiState.value.copy(
            currentStep = OnboardingStep.FINAL
        )
    }

    fun onWriteFinalDataClicked() = viewModelScope.launch {
        val result = backupHardwareWalletUseCase.proceedBackup()
        when (result) {
            is CompletionResult.Success -> {
                if (!validateBackUseCase.isValidBackupStatus(result.data)) {
                    _errorEvents.trySend(HardwareWalletError.ErrorInBackupCard)
                }
                val lastScanResponse = tangemCreateWalletUseCase.tangemSdkManager.lastScanResponse
                if (backupHardwareWalletUseCase.isBackupFinished() && lastScanResponse != null && accountName != null) {
                    createHardwareWalletUseCase(
                        accountName = accountName!!,
                        // added backup count to response
                        scanResponse = lastScanResponse.copy(
                            card = lastScanResponse.card.copy(
                                backupStatus = result.data.backupStatus
                            )
                        )
                    )
                    _uiState.value = _uiState.value.copy(
                        success = true
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        cardNumToBackup = uiState.value.cardNumToBackup + 1
                    )
                }
            }

            is CompletionResult.Failure -> {
                when (val error = result.error) {
                    is TangemSdkError.BackupFailedNotEmptyWallets -> {
                        _errorEvents.trySend(HardwareWalletError.NeedFactoryReset(error.cardId))
                    }
                }
            }
        }
    }

    fun resetCard(cardId: String) = viewModelScope.launch {
        resetToFactorySettingsUseCase.resetPrimaryCard(cardId, false)
    }
}

internal enum class OnboardingStep(val progress: Float) {
    CREATE_WALLET(0.25f),
    ADD_BACKUP(0.5f),
    CREATE_ACCESS_CODE(0.75f),
    FINAL(1.0f)
}