package cash.p.terminal.tangem.domain.task

import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.extensions.guard
import com.tangem.operations.backup.ResetBackupCommand
import com.tangem.operations.wallet.PurgeWalletCommand

class ResetToFactorySettingsTask(
    override val allowsRequestAccessCodeFromRepository: Boolean,
) : CardSessionRunnable<Boolean> {

    private var isResetCompleted = false

    override fun run(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        deleteWallets(session, callback)
    }

    private fun deleteWallets(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        val wallet = session.environment.card?.wallets?.lastOrNull().guard {
            resetBackup(session, callback)
            return
        }

        PurgeWalletCommand(wallet.publicKey).run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    isResetCompleted = true
                    deleteWallets(session, callback)
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }

    private fun resetBackup(session: CardSession, callback: (result: CompletionResult<Boolean>) -> Unit) {
        if (session.environment.card?.backupStatus == null ||
            session.environment.card?.backupStatus == Card.BackupStatus.NoBackup
        ) {
            callback(CompletionResult.Success(isResetCompleted))
            return
        }

        ResetBackupCommand().run(session) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    callback(CompletionResult.Success(true))
                }
                is CompletionResult.Failure -> callback(CompletionResult.Failure(result.error))
            }
        }
    }
}
