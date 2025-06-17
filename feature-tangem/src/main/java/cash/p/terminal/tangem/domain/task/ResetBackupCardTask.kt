package cash.p.terminal.tangem.domain.task

import com.tangem.common.CompletionResult
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.CompletionCallback
import com.tangem.operations.PreflightReadMode
import com.tangem.operations.PreflightReadTask

/**
 * Task for resetting backup card.
 *
 * 1. Read card and check if card is corresponding to expected user wallet id.
 * 2. Reset card.
 *
 */
internal class ResetBackupCardTask(
    private val firstWalletPublicKey: ByteArray,
) : CardSessionRunnable<Pair<ByteArray?, Boolean>> {

    override val allowsRequestAccessCodeFromRepository: Boolean = false

    override fun run(session: CardSession, callback: CompletionCallback<Pair<ByteArray?, Boolean>>) {
        PreflightReadTask(
            readMode = PreflightReadMode.FullCardRead,
            filter = UserWalletIdPreflightReadFilter(expectedPublicKey = firstWalletPublicKey),
        ).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> resetCard(session, callback)
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetCard(session: CardSession, callback: CompletionCallback<Pair<ByteArray?, Boolean>>) {
        ResetToFactorySettingsTask(allowsRequestAccessCodeFromRepository).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> callback(CompletionResult.Success(result.data))
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
