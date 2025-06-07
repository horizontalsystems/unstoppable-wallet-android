package cash.p.terminal.tangem.domain.task

import cash.p.terminal.tangem.domain.RING_BATCH_IDS
import cash.p.terminal.tangem.domain.RING_BATCH_PREFIX
import cash.p.terminal.tangem.domain.card.CardConfig
import cash.p.terminal.tangem.domain.derivation.DerivationConfig
import cash.p.terminal.tangem.domain.getDerivationStyle
import cash.p.terminal.tangem.domain.model.BlockchainToDerive
import cash.p.terminal.tangem.domain.model.ProductType
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.tangem.domain.usecase.CollectDerivationsUseCase
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.operations.backup.PrimaryCard
import com.tangem.operations.derivation.DeriveMultipleWalletPublicKeysTask
import io.horizontalsystems.core.FeatureCoroutineExceptionHandler
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class ScanWalletProcessor(
    private val blockchainsToDerive: List<TokenQuery>,
) : ProductCommandProcessor<ScanResponse> {
    private val collectDerivationsUseCase: CollectDerivationsUseCase = CollectDerivationsUseCase()

    private val coroutineContext: CoroutineContext
        get() = Job() + Dispatchers.IO + FeatureCoroutineExceptionHandler.create("ScanWalletProcessor")
    val scope = CoroutineScope(coroutineContext)

    private val mainCoroutineContext: CoroutineContext
        get() = Job() + Dispatchers.Main + FeatureCoroutineExceptionHandler.create("ScanWalletProcessor")
    val mainScope = CoroutineScope(mainCoroutineContext)

    var primaryCard: PrimaryCard? = null
    override fun proceed(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        startLinkingForBackupIfNeeded(card, session, callback)
    }

    private fun startLinkingForBackupIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        mainScope.launch {
            deriveKeysIfNeeded(card, session, callback)
        }
    }

    private fun deriveKeysIfNeeded(
        card: Card,
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit,
    ) {
        val productType = getWalletProductType(card)
        val config = CardConfig.createConfig(card)
        scope.launch {
            val scanResponse = ScanResponse(
                card = card,
                productType = productType,
                walletData = session.environment.walletData,
                primaryCard = primaryCard,
            )

            val derivations =  collectDerivationsUseCase(card, config, blockchainsToDerive)
            if (derivations.isEmpty() || !card.settings.isHDWalletAllowed) {
                callback(CompletionResult.Success(scanResponse))
                return@launch
            }
            DeriveMultipleWalletPublicKeysTask(derivations).run(session) { result ->
                when (result) {
                    is CompletionResult.Success -> {
                        val response = scanResponse.copy(derivedKeys = result.data.entries)
                        callback(CompletionResult.Success(response))
                    }

                    is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
                }
            }
        }
    }

    private fun getWalletProductType(card: Card): ProductType {
        return when {
            RING_BATCH_IDS.contains(card.batchId) || card.batchId.startsWith(RING_BATCH_PREFIX) -> ProductType.Ring
            card.firmwareVersion >= FirmwareVersion.Ed25519Slip0010Available &&
                    card.settings.isKeysImportAllowed -> ProductType.Wallet2

            else -> ProductType.Wallet
        }
    }

}
