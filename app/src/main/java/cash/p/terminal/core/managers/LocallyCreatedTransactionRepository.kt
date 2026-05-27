package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.LocallyCreatedTransactionStorage
import cash.p.terminal.entities.LocallyCreatedTransactionRecord
import cash.p.terminal.entities.PendingTransactionEntity
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

class LocallyCreatedTransactionRepository(
    private val storage: LocallyCreatedTransactionStorage,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val mutex = Mutex()
    private val readFailureLogged = AtomicBoolean(false)
    private val _changedFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changedFlow: SharedFlow<Unit> = _changedFlow.asSharedFlow()

    suspend fun isCreated(record: TransactionRecord): Boolean {
        return isCreated(
            accountId = record.source.account.id,
            blockchainTypeUid = record.blockchainType.uid,
            transactionHash = record.transactionHash,
        )
    }

    suspend fun isCreated(
        accountId: String,
        blockchainTypeUid: String,
        transactionHash: String?,
    ): Boolean {
        val normalizedHash = normalizeHash(transactionHash) ?: return false
        return withContext(dispatcherProvider.io) {
            try {
                storage.exists(accountId, blockchainTypeUid, normalizedHash)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logReadFailureOnce(error)
                false
            }
        }
    }

    suspend fun markCreated(wallet: Wallet, transactionHash: String?) {
        markCreated(
            accountId = wallet.account.id,
            blockchainTypeUid = wallet.token.blockchainType.uid,
            transactionHash = transactionHash,
        )
    }

    suspend fun markCreated(entity: PendingTransactionEntity, transactionHash: String?) {
        markCreated(
            accountId = entity.walletId,
            blockchainTypeUid = entity.blockchainTypeUid,
            transactionHash = transactionHash,
            createdAt = entity.createdAt,
        )
    }

    suspend fun markCreated(record: TransactionRecord) {
        markCreated(
            accountId = record.source.account.id,
            blockchainTypeUid = record.blockchainType.uid,
            transactionHash = record.transactionHash,
            createdAt = record.timestamp * 1000,
        )
    }

    suspend fun markCreated(
        accountId: String,
        blockchainTypeUid: String,
        transactionHash: String?,
        createdAt: Long = System.currentTimeMillis(),
    ) {
        val normalizedHash = normalizeHash(transactionHash) ?: return

        runBestEffort("mark locally created transaction") {
            withContext(NonCancellable + dispatcherProvider.io) {
                mutex.withLock {
                    val inserted = storage.insert(
                        LocallyCreatedTransactionRecord(
                            accountId = accountId,
                            blockchainTypeUid = blockchainTypeUid,
                            transactionHash = normalizedHash,
                            createdAt = createdAt,
                        )
                    )
                    if (inserted) {
                        trimAccountIfNeeded(accountId)
                        _changedFlow.tryEmit(Unit)
                    }
                }
            }
        }
    }

    suspend fun trimAllAccounts() = runBestEffort("trim locally created transactions") {
        mutex.withLock {
            withContext(dispatcherProvider.io) {
                storage.trimAllAccounts(MAX_RECORDS_PER_ACCOUNT)
            }
        }
    }

    suspend fun deleteByAccountIds(accountIds: List<String>) =
        runBestEffort("delete locally created transactions") {
            mutex.withLock {
                withContext(dispatcherProvider.io) {
                    storage.deleteByAccountIds(accountIds)
                }
                _changedFlow.tryEmit(Unit)
            }
        }

    suspend fun count(accountId: String): Int = withContext(dispatcherProvider.io) {
        storage.count(accountId)
    }

    private fun normalizeHash(transactionHash: String?): String? {
        return transactionHash?.trim()?.takeIf { it.isNotEmpty() }
    }

    private suspend fun trimAccountIfNeeded(accountId: String) {
        if (storage.count(accountId) > MAX_RECORDS_PER_ACCOUNT) {
            storage.trimAccount(accountId, MAX_RECORDS_PER_ACCOUNT)
        }
    }

    private suspend fun runBestEffort(action: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, "Failed to $action")
        }
    }

    private fun logReadFailureOnce(error: Exception) {
        if (readFailureLogged.compareAndSet(false, true)) {
            Timber.e(error, "Failed to read locally created transaction")
        }
    }

    companion object {
        const val MAX_RECORDS_PER_ACCOUNT = 20_000
    }
}
