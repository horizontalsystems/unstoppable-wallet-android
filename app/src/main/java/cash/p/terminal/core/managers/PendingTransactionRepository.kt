package cash.p.terminal.core.managers

import cash.p.terminal.core.storage.PendingTransactionStorage
import cash.p.terminal.entities.PendingTransactionDraft
import cash.p.terminal.entities.PendingTransactionEntity
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class PendingTransactionRepository(
    private val storage: PendingTransactionStorage,
    private val dispatcherProvider: DispatcherProvider,
    private val backgroundManager: BackgroundManager,
) {
    private val mutex = Mutex()
    private val jobMutex = Mutex()

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    private var periodicJob: Job? = null

    init {
        scope.launch(dispatcherProvider.default) {
            backgroundManager.stateFlow.collect { state ->
                when (state) {
                    BackgroundManagerState.EnterForeground -> {
                        startCleanupJob()
                    }

                    BackgroundManagerState.EnterBackground -> {
                        stopJob()
                    }

                    else -> Unit
                }
            }
        }
        startCleanupJob()
    }

    suspend fun insert(draft: PendingTransactionDraft): PendingTransactionEntity = mutex.withLock {
        withContext(dispatcherProvider.io) {
            val entity = draftToEntity(draft)
            storage.insert(entity)
            startCleanupJob()
            entity
        }
    }

    suspend fun updateTxId(draftId: String, txId: String): PendingTransactionEntity? = mutex.withLock {
        withContext(dispatcherProvider.io) {
            storage.updateTxId(draftId, txId)
            storage.getById(draftId)
        }
    }

    fun getActivePendingFlow(walletId: String): Flow<List<PendingTransactionEntity>> =
        storage.getActivePendingFlow(walletId)

    suspend fun getPendingForWallet(walletId: String): List<PendingTransactionEntity> =
        withContext(dispatcherProvider.io) {
            storage.getPendingForWallet(walletId)
        }

    suspend fun markBalanceConfirmed(ids: List<String>) = mutex.withLock {
        withContext(dispatcherProvider.io) {
            storage.markBalanceConfirmed(ids, System.currentTimeMillis())
            startCleanupJob()
        }
    }

    suspend fun deleteById(id: String) = mutex.withLock {
        withContext(dispatcherProvider.io) {
            storage.deleteById(id)
            startCleanupJob()
        }
    }

    suspend fun deleteByIds(ids: List<String>) = mutex.withLock {
        withContext(dispatcherProvider.io) {
            storage.deleteByIds(ids)
            startCleanupJob()
        }
    }

    private fun draftToEntity(draft: PendingTransactionDraft): PendingTransactionEntity {
        return PendingTransactionEntity(
            id = draft.id,
            walletId = draft.wallet.account.id,
            coinUid = draft.token.coin.uid,
            blockchainTypeUid = draft.token.blockchainType.uid,
            tokenTypeId = draft.token.type.id,
            meta = draft.meta,
            amountAtomic = draft.amount.movePointRight(draft.token.decimals).toBigInteger()
                .toString(),
            feeAtomic = draft.fee?.movePointRight(draft.token.decimals)?.toBigInteger()?.toString(),
            sdkBalanceAtCreationAtomic = draft.sdkBalanceAtCreation
                .movePointRight(draft.token.decimals).toBigInteger().toString(),
            fromAddress = draft.fromAddress,
            toAddress = draft.toAddress,
            txHash = draft.txHash,
            nonce = draft.nonce,
            memo = draft.memo,
            createdAt = draft.timestamp,
            expiresAt = draft.timestamp + TimeUnit.HOURS.toMillis(1),
        )
    }

    private suspend fun cleanupExpired(): Boolean = withContext(dispatcherProvider.io) {
        val expired = storage.getExpired()
        if (expired.isEmpty()) return@withContext false

        storage.deleteByIds(expired.map { it.id })

        return@withContext true
    }

    private suspend fun hasPendingTransactions(): Boolean =
        withContext(dispatcherProvider.io) {
            storage.hasPendingTransactions()
        }

    private fun startCleanupJob() {
        scope.launch(dispatcherProvider.default) {
            jobMutex.withLock {
                if (hasPendingTransactions()) {
                    if (periodicJob != null) return@launch
                    periodicJob = scope.launch {
                        while (isActive) {
                            delay(30_000) // Every 30 seconds

                            val removedSomething = cleanupExpired()
                            if (removedSomething && !hasPendingTransactions()) {
                                stopJob()
                                break
                            }
                        }
                        periodicJob = null
                    }
                } else {
                    stopJob()
                }
            }
        }
    }

    private fun stopJob() {
        periodicJob?.cancel()
        periodicJob = null
    }
}
