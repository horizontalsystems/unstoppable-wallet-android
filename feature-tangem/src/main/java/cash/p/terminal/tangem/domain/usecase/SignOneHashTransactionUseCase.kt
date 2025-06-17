package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.Message
import com.tangem.crypto.hdWallet.DerivationPath

class SignOneHashTransactionUseCase(private val tangemSdkManager: TangemSdkManager) {

    suspend operator fun invoke(
        hash: ByteArray,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        message: Message? = null
    ) = tangemSdkManager.sign(
            cardId = null,
            hash = hash,
            walletPublicKey = walletPublicKey,
            derivationPath = derivationPath,
            message = message,
        )
}