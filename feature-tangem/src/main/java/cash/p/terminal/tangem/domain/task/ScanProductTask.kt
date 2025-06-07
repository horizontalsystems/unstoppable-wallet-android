package cash.p.terminal.tangem.domain.task

import cash.p.terminal.tangem.domain.isExcluded
import cash.p.terminal.tangem.domain.isTangemTwins
import cash.p.terminal.tangem.domain.model.ScanResponse
import cash.p.terminal.wallet.entities.TokenQuery
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.guard
import com.tangem.operations.ScanTask

internal class ScanProductTask(
    private val card: Card?,
    private val blockchainsToDerive: List<TokenQuery>,
    override val allowsRequestAccessCodeFromRepository: Boolean = false,
) : CardSessionRunnable<ScanResponse> {

    override fun run(
        session: CardSession,
        callback: (result: CompletionResult<ScanResponse>) -> Unit
    ) {
        val card = this.card ?: session.environment.card.guard {
            callback(CompletionResult.Failure(TangemSdkError.MissingPreflightRead()))
            return
        }

        val error = getErrorIfExcludedCard(card)
        if (error != null) {
            callback(CompletionResult.Failure(error))
            return
        }

        val commandProcessor = when {
            card.isTangemTwins() -> ScanTwinProcessor()
            else -> ScanWalletProcessor(blockchainsToDerive)
        }
        commandProcessor.proceed(card, session) { processorResult ->
            when (processorResult) {
                is CompletionResult.Success -> ScanTask().run(session) { scanTaskResult ->
                    when (scanTaskResult) {
                        is CompletionResult.Success -> {
                            callback(CompletionResult.Success(processorResult.data))
                        }

                        is CompletionResult.Failure -> callback(
                            CompletionResult.Failure(
                                scanTaskResult.error
                            )
                        )
                    }
                }

                is CompletionResult.Failure -> callback(CompletionResult.Failure(processorResult.error))
            }
        }
    }

    private fun getErrorIfExcludedCard(card: Card): TangemError? {
        if (card.isExcluded()) return TangemSdkError.WrongCardType()
        if (card.firmwareVersion < FirmwareVersion.Ed25519Slip0010Available &&
            card.wallets.any { it.isImported }
        ) {
            return TangemSdkError.WrongCardType()
        }
        return null
    }
}
