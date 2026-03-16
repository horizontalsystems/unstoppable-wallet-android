package io.horizontalsystems.bankwallet.modules.multiswap.history

import io.horizontalsystems.bankwallet.core.IAccountManager
import io.horizontalsystems.bankwallet.core.storage.SwapRecordDao
import io.horizontalsystems.bankwallet.entities.SwapRecord
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

    fun save(record: SwapRecord) {
        swapRecordDao.insert(record)
        _recordsUpdatedFlow.tryEmit(Unit)
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

    fun updateStatus(id: Int, status: SwapStatus) {
        swapRecordDao.updateStatus(id, status.name)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateStatusAndAmountOut(id: Int, status: SwapStatus, amountOut: String) {
        swapRecordDao.updateStatusAndAmountOut(id, status.name, amountOut)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateTransactionHash(id: Int, hash: String) {
        swapRecordDao.updateTransactionHash(id, hash)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun updateOutboundTransactionHash(id: Int, hash: String) {
        swapRecordDao.updateOutboundTransactionHash(id, hash)
        _recordsUpdatedFlow.tryEmit(Unit)
    }
}