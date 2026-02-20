package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.storage.SwapRecordDao
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SwapRecordManager(private val swapRecordDao: SwapRecordDao) {

    private val _recordsUpdatedFlow = MutableSharedFlow<Unit>(replay = 1)
    val recordsUpdatedFlow = _recordsUpdatedFlow.asSharedFlow()

    fun save(record: SwapRecord) {
        swapRecordDao.insert(record)
        _recordsUpdatedFlow.tryEmit(Unit)
    }

    fun getAll(): List<SwapRecord> {
        return swapRecordDao.getAll()
    }

    fun getPending(): List<SwapRecord> {
        return swapRecordDao.getPending()
    }

    fun getById(id: Int): SwapRecord? {
        return swapRecordDao.getById(id)
    }

    fun updateStatus(id: Int, status: SwapStatus) {
        swapRecordDao.updateStatus(id, status.name)
        _recordsUpdatedFlow.tryEmit(Unit)
    }
}
