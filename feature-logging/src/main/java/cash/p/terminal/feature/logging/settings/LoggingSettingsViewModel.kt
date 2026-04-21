package cash.p.terminal.feature.logging.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.wallet.managers.UserManager
import io.horizontalsystems.core.ILoginRecordRepository
import io.horizontalsystems.core.ILoggingSettings
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.entities.AutoDeletePeriod
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class LoggingSettingsViewModel(
    private val loggingSettings: ILoggingSettings,
    private val loginRecordRepository: ILoginRecordRepository,
    private val userManager: UserManager,
    private val pinComponent: IPinComponent,
    checkPremiumUseCase: CheckPremiumUseCase
) : ViewModel() {

    private val currentLevel: Int
        get() = userManager.getUserLevel()

    var uiState by mutableStateOf(
        LoggingSettingsUiState(
            logSuccessfulLoginsEnabled = loggingSettings.getLogSuccessfulLoginsEnabled(currentLevel),
            selfieOnSuccessfulLoginEnabled = loggingSettings.getSelfieOnSuccessfulLoginEnabled(currentLevel),
            logUnsuccessfulLoginsEnabled = loggingSettings.getLogUnsuccessfulLoginsEnabled(currentLevel),
            selfieOnUnsuccessfulLoginEnabled = loggingSettings.getSelfieOnUnsuccessfulLoginEnabled(currentLevel),
            passcodeEnabled = pinComponent.isLogLoggingPinSet(),
            deleteContactsPasscodeEnabled = pinComponent.isDeleteContactsPinSet(),
            logIntoDuressModeEnabled = loggingSettings.getLogIntoDuressModeEnabled(currentLevel),
            selfieOnDuressLoginEnabled = loggingSettings.getSelfieOnDuressLoginEnabled(currentLevel),
            deleteAllAuthDataOnDuressEnabled = loggingSettings.getDeleteAllAuthDataOnDuressEnabled(currentLevel),
            autoDeletePeriod = AutoDeletePeriod.fromValue(loggingSettings.getAutoDeleteLogsPeriod(currentLevel)),
            deleteButtonEnabled = false,
            isPremiumActive = checkPremiumUseCase.getPremiumType().isPremium()
        )
    )
        private set

    init {
        viewModelScope.launch {
            loginRecordRepository.observeHasRecords(userManager.getUserLevel())
                .collect { hasRecords ->
                    uiState = uiState.copy(deleteButtonEnabled = hasRecords)
                }
        }
        viewModelScope.launch {
            pinComponent.pinSetFlowable.asFlow().collect {
                updatePasscodeLoggingState()
            }
        }
    }

    fun setSelfieOnSuccessfulLoginEnabled(enabled: Boolean) {
        loggingSettings.setSelfieOnSuccessfulLoginEnabled(currentLevel, enabled)
        uiState = uiState.copy(selfieOnSuccessfulLoginEnabled = enabled)
    }

    fun setLogSuccessfulLoginsEnabled(enabled: Boolean) {
        loggingSettings.setLogSuccessfulLoginsEnabled(currentLevel, enabled)
        uiState = uiState.copy(logSuccessfulLoginsEnabled = enabled)
    }

    fun setLogUnsuccessfulLoginsEnabled(enabled: Boolean) {
        loggingSettings.setLogUnsuccessfulLoginsEnabled(currentLevel, enabled)
        uiState = uiState.copy(logUnsuccessfulLoginsEnabled = enabled)
    }

    fun setSelfieOnUnsuccessfulLoginEnabled(enabled: Boolean) {
        loggingSettings.setSelfieOnUnsuccessfulLoginEnabled(currentLevel, enabled)
        uiState = uiState.copy(selfieOnUnsuccessfulLoginEnabled = enabled)
    }

    fun setLogIntoDuressModeEnabled(enabled: Boolean) {
        loggingSettings.setLogIntoDuressModeEnabled(currentLevel, enabled)
        uiState = uiState.copy(logIntoDuressModeEnabled = enabled)
    }

    fun onSelfieOnDuressLoginEnabled(enabled: Boolean) {
        loggingSettings.setSelfieOnDuressLoginEnabled(currentLevel, enabled)
        uiState = uiState.copy(selfieOnDuressLoginEnabled = enabled)
    }

    fun setDeleteAllAuthDataOnDuressEnabled(enabled: Boolean) {
        loggingSettings.setDeleteAllAuthDataOnDuressEnabled(currentLevel, enabled)
        uiState = uiState.copy(deleteAllAuthDataOnDuressEnabled = enabled)
    }

    fun setAutoDeletePeriod(period: AutoDeletePeriod) {
        loggingSettings.setAutoDeleteLogsPeriod(currentLevel, period.value)
        uiState = uiState.copy(autoDeletePeriod = period)
    }

    fun deleteAllLogs() {
        viewModelScope.launch {
            loginRecordRepository.deleteAll(userManager.getUserLevel())
        }
    }

    fun disablePasscodeLoggingPin() {
        pinComponent.disableLogLoggingPin()
        updatePasscodeLoggingState()
    }

    fun disableDeleteContactsPin() {
        pinComponent.disableDeleteContactsPin()
        updatePasscodeLoggingState()
    }

    fun updatePasscodeLoggingState() {
        uiState = uiState.copy(
            passcodeEnabled = pinComponent.isLogLoggingPinSet(),
            deleteContactsPasscodeEnabled = pinComponent.isDeleteContactsPinSet()
        )
    }
}

data class LoggingSettingsUiState(
    val logSuccessfulLoginsEnabled: Boolean,
    val isPremiumActive: Boolean,
    val passcodeEnabled: Boolean = false,
    val deleteContactsPasscodeEnabled: Boolean = false,
    val selfieOnSuccessfulLoginEnabled: Boolean = false,
    val logUnsuccessfulLoginsEnabled: Boolean = false,
    val selfieOnUnsuccessfulLoginEnabled: Boolean = false,
    val logIntoDuressModeEnabled: Boolean = false,
    val selfieOnDuressLoginEnabled: Boolean = false,
    val deleteAllAuthDataOnDuressEnabled: Boolean = false,
    val autoDeletePeriod: AutoDeletePeriod = AutoDeletePeriod.YEAR,
    val deleteButtonEnabled: Boolean = false,
    val noCameraPermissions: Boolean = false,
)
