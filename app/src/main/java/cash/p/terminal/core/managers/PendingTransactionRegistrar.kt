package cash.p.terminal.core.managers

import cash.p.terminal.entities.PendingTransactionDraft
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

interface PendingTransactionRegistrar {
    /**
     * Registers a pending transaction after successful broadcast
     * @return Transaction ID
     */
    suspend fun register(draft: PendingTransactionDraft): String

    /**
     * Updates the transaction ID
     */
    suspend fun updateTxId(draftId: String, txId: String)

    /**
     * Deletes a transaction when broadcast fails
     */
    suspend fun deleteFailed(id: String)
}

class PendingTransactionRegistrarImpl(
    private val repository: PendingTransactionRepository,
    private val pendingBalanceCalculator: PendingBalanceCalculator,
    private val locallyCreatedTransactionRepository: LocallyCreatedTransactionRepository,
) : PendingTransactionRegistrar {

    override suspend fun register(draft: PendingTransactionDraft): String {
        val entity = repository.insert(draft)
        pendingBalanceCalculator.onPendingInserted(entity)
        return draft.id
    }

    override suspend fun updateTxId(draftId: String, txId: String) {
        withContext(NonCancellable) {
            repository.updateTxId(draftId, txId)?.let { entity ->
                locallyCreatedTransactionRepository.markCreated(entity, txId)
            }
        }
    }

    override suspend fun deleteFailed(id: String) {
        repository.deleteById(id)
    }
}
