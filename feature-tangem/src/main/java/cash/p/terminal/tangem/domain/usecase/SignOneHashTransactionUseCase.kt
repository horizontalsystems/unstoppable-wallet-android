package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.Message
import com.tangem.crypto.hdWallet.DerivationPath

class SignOneHashTransactionUseCase(private val tangemSdkManager: TangemSdkManager) {

    suspend operator fun invoke(
        cardId: String?,
        hash: ByteArray,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        message: Message? = null
    ) = tangemSdkManager.sign(
            cardId = cardId,
            hash = hash,
            walletPublicKey = walletPublicKey,
            derivationPath = derivationPath,
            message = message,
        )
}