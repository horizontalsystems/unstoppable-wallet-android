package cash.p.terminal.core.storage

import cash.p.terminal.entities.PendingMultiSwap
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.flow.Flow
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

    fun getAll(): Flow<List<PendingMultiSwap>> = dao.getAll()

    suspend fun getAllOnce(): List<PendingMultiSwap> = withContext(dispatcherProvider.io) {
        dao.getAllOnce()
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
