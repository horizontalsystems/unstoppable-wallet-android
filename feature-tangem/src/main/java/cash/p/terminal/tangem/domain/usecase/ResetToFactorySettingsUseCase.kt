package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager

class ResetToFactorySettingsUseCase(
    private val tangemSdkManager: TangemSdkManager
) {
    suspend operator fun invoke(
        cardId: String,
        allowsRequestAccessCodeFromRepository: Boolean
    ) = tangemSdkManager.resetToFactorySettings(cardId, allowsRequestAccessCodeFromRepository)
}