package cash.p.terminal.core.storage

import cash.p.terminal.entities.PendingMultiSwap
import cash.p.terminal.wallet.ActiveAccountState
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class PendingMultiSwapStorage(
    private val dao: PendingMultiSwapDao,
    private val dispatcherProvider: DispatcherProvider
) {
    private companion object {
        val MAX_AGE_MS = TimeUnit.DAYS.toMillis(30)
    }

    suspend fun cleanup() = withContext(dispatcherProvider.io) {
        dao.deleteOlderThan(System.currentTimeMillis() - MAX_AGE_MS)
    }

    fun getByAccountId(accountId: String): Flow<List<PendingMultiSwap>> =
        dao.getByAccountId(accountId)

    fun observeForActiveAccount(
        activeAccountStateFlow: Flow<ActiveAccountState>
    ): Flow<List<PendingMultiSwap>> =
        activeAccountStateFlow.flatMapLatest { state ->
            val accountId = (state as? ActiveAccountState.ActiveAccount)?.account?.id
            if (accountId != null) getByAccountId(accountId) else flowOf(emptyList())
        }

    suspend fun getAllOnceByAccountId(accountId: String): List<PendingMultiSwap> =
        withContext(dispatcherProvider.io) {
            dao.getAllOnceByAccountId(accountId)
        }

    suspend fun getById(id: String): PendingMultiSwap? = withContext(dispatcherProvider.io) {
        dao.getById(id)
    }

    suspend fun insert(swap: PendingMultiSwap) = withContext(dispatcherProvider.io) {
        dao.insert(swap)
    }

    suspend fun updateLeg1(
        id: String,
        status: String,
        amountOut: BigDecimal?,
        transactionId: String?
    ) = withContext(dispatcherProvider.io) {
        dao.updateLeg1(id, status, amountOut, transactionId)
    }

    suspend fun updateLeg2(
        id: String,
        status: String,
        amountOut: BigDecimal?,
        transactionId: String?
    ) = withContext(dispatcherProvider.io) {
        dao.updateLeg2(id, status, amountOut, transactionId)
    }

    suspend fun setLeg1ProviderTransactionId(id: String, providerTransactionId: String) =
        withContext(dispatcherProvider.io) {
            dao.setLeg1ProviderTransactionId(id, providerTransactionId)
        }

    suspend fun setLeg1InfoRecordUid(id: String, recordUid: String) =
        withContext(dispatcherProvider.io) {
            dao.setLeg1InfoRecordUid(id, recordUid)
        }

    suspend fun delete(id: String) = withContext(dispatcherProvider.io) {
        dao.delete(id)
    }

    fun count(): Flow<Int> = dao.count()
}
