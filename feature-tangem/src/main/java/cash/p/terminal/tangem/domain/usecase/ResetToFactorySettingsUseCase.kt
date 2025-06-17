package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager

class ResetToFactorySettingsUseCase(
    private val tangemSdkManager: TangemSdkManager
) {
    suspend fun resetPrimaryCard(
        cardId: String?,
        allowsRequestAccessCodeFromRepository: Boolean
    ) = tangemSdkManager.resetToFactorySettings(cardId, allowsRequestAccessCodeFromRepository)

    suspend fun resetBackupCard(
        cardNumber: Int,
        firstWalletPublicKey: ByteArray
    ) = tangemSdkManager.resetBackupCard(
        cardNumber = cardNumber,
        firstWalletPublicKey = firstWalletPublicKey
    )
}