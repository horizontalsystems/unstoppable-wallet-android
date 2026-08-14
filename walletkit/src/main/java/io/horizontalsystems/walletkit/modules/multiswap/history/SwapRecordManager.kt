package io.horizontalsystems.walletkit.modules.multiswap.history

import io.horizontalsystems.walletkit.core.IAccountManager
import io.horizontalsystems.walletkit.core.storage.SwapRecordDao
import io.horizontalsystems.walletkit.entities.SwapRecord
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SwapRecordManager(
    private val accountManager: IAccountManager,
    private val swapRecordDao: SwapRecordDao,
) {

    private val _recordsUpdatedFlow = MutableSharedFlow<Unit>(replay = 1)
    val recordsUpdatedFlow = _recordsUpdatedFlow.asSharedFlow()

    init {
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun save(record: SwapRecord): Int {
        val rowId = swapRecordDao.insert(record)
        _recordsUpdatedFlow.tryEmit(Unit)
        return rowId.toInt()
    }

    fun getAll(): List<SwapRecord> {
        val accountId = accountManager.activeAccount?.id ?: return emptyList()
        return swapRecordDao.getAll(accountId)
    }

    fun getPending(): List<SwapRecord> {
        val accountId = accountManager.activeAccount?.id ?: return emptyList()
        return swapRecordDao.getPending(accountId)
    }

    fun getById(id: Int): SwapRecord? {
        return swapRecordDao.getById(id)
    }

    fun getByTrackingHandle(trackingHandle: String): SwapRecord? {
        return swapRecordDao.getByTrackingHandle(trackingHandle)
    }

    fun updateStatus(id: Int, status: SwapStatus, pauseReason: String?) {
        swapRecordDao.updateStatus(id, status.name, pauseReason)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateStatusAndAmountOut(id: Int, status: SwapStatus, amountOut: String, pauseReason: String?) {
        swapRecordDao.updateStatusAndAmountOut(id, status.name, amountOut, pauseReason)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateTransactionHash(id: Int, hash: String) {
        swapRecordDao.updateTransactionHash(id, hash)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    /**
     * Releases the record from app-side tracking: with the handle gone, the
     * server-side sync loop owns the record's advancement.
     */
    fun clearTrackingHandle(id: Int) {
        swapRecordDao.clearTrackingHandle(id)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateOutboundTransactionHash(id: Int, hash: String) {
        swapRecordDao.updateOutboundTransactionHash(id, hash)
        _recordsUpdatedFlow.tryEmit(Unit)
    }
}