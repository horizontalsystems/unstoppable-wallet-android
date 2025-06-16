package cash.p.terminal.tangem.ui

import com.tangem.common.card.Card

internal data class HardwareWalletOnboardingUIState(
    val currentStep: OnboardingStep = OnboardingStep.CREATE_WALLET,
    val backupCards: List<Card> = emptyList(),
    val primaryCardId: String? = null,
    val cardNumToBackup: Int = -1,
    val success: Boolean = false,
)

sealed interface HardwareWalletError {
    object CardNotActivated : HardwareWalletError
    object WalletsNotCreated : HardwareWalletError
    object UnknownError : HardwareWalletError
    object AttestationFailed : HardwareWalletError
    object ErrorInBackupCard : HardwareWalletError
    data class NeedFactoryReset(val cardId: String) : HardwareWalletError
}
