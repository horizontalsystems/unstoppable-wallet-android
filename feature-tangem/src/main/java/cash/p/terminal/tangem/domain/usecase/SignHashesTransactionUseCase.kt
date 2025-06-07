package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import com.tangem.Message
import com.tangem.crypto.hdWallet.DerivationPath

class SignHashesTransactionUseCase(private val tangemSdkManager: TangemSdkManager) {

    suspend operator fun invoke(
        cardId: String?,
        hashes: Array<ByteArray>,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath?,
        message: Message? = null
    ) = tangemSdkManager.sign(
        cardId = cardId,
        hashes = hashes,
        walletPublicKey = walletPublicKey,
        derivationPath = derivationPath,
        message = message,
    )
}