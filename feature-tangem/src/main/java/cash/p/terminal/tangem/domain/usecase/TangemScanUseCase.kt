package cash.p.terminal.tangem.domain.usecase

import cash.p.terminal.tangem.domain.TangemConfig
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.sdk.TangemSdkManager
import cash.p.terminal.wallet.IHardwarePublicKeyStorage
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.useCases.ScanToAddUseCase
import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.operations.derivation.DerivationTaskResponse

class TangemScanUseCase(
    private val tangemSdkManager: TangemSdkManager,
    private val hardwarePublicKeyStorage: IHardwarePublicKeyStorage
) : ScanToAddUseCase {

    suspend fun scanProduct(
        blockchainsToDerive: List<TokenQuery> = emptyList(),
        message: Message? = null,
        cardId: String? = null,
    ): CompletionResult<ScanResponse> = tangemSdkManager.scanProduct(
        cardId = cardId,
        blockchainsToDerive = blockchainsToDerive,
        message = message,
    )

    override suspend fun addTokensByScan(
        blockchainsToDerive: List<TokenQuery>,
        cardId: String,
        accountId: String
    ): Boolean {
        val filtered = blockchainsToDerive.filter {
            !TangemConfig.isExcludedForHardwareCard(it)
        }
        val scanResponse = scanProduct(
            blockchainsToDerive = filtered,
            cardId = cardId
        )
        if (scanResponse is CompletionResult.Success) {
            val publicKeys =
                BuildHardwarePublicKeyUseCase().invoke(
                    scanResponse = scanResponse.data,
                    accountId = accountId,
                    blockchainTypeList = filtered
                )
            hardwarePublicKeyStorage.save(publicKeys)
            return blockchainsToDerive.size == publicKeys.size
        } else {
            return false
        }
    }

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