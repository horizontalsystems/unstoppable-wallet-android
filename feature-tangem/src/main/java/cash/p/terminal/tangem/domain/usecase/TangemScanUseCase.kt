package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import cash.p.terminal.wallet.entities.TokenQuery
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.derivation.DerivationTaskResponse

class TangemScanUseCase(private val tangemSdkManager: TangemSdkManager) {

    suspend fun scanProduct(
        blockchainsToDerive: List<TokenQuery> = emptyList(),
        message: Message? = null,
        cardId: String? = null,
    ): CompletionResult<ScanResponse> = tangemSdkManager.scanProduct(
        cardId = cardId,
        blockchainsToDerive = blockchainsToDerive,
        message = message,
    )

    /*suspend fun deriveExtendedPublicKey(
        cardId: String,
        walletPublicKey: ByteArray,
        derivationPath: DerivationPath
    ): CompletionResult<ExtendedPublicKey> {
        return tangemSdkManager.deriveExtendedPublicKey(
            cardId = cardId,
            walletPublicKey = walletPublicKey,
            derivation = derivationPath
        )
    }*/

    suspend fun derivePublicKeys(
        cardId: String?,
        derivations: Map<ByteArrayKey, List<DerivationPath>>
    ): CompletionResult<DerivationTaskResponse> {
        return tangemSdkManager.derivePublicKeys(
            cardId = cardId,
            derivations = derivations,
            preflightReadFilter = null
        )
    }
}