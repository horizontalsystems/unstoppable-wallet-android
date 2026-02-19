package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.storage.SwapRecordDao

class SwapRecordManager(private val swapRecordDao: SwapRecordDao) {

    fun save(record: SwapRecord) {
        swapRecordDao.insert(record)
    }

    fun getAll(): List<SwapRecord> {
        return swapRecordDao.getAll()
    }

    fun getById(id: Int): SwapRecord? {
        return swapRecordDao.getById(id)
    }

    fun updateStatus(id: Int, status: SwapStatus) {
        swapRecordDao.updateStatus(id, status.name)
    }
}
