package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.ZanoNodeRecord

class ZanoNodeStorage(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.zanoNodeDao() }

    fun getAll() = dao.getAll()

    fun save(record: ZanoNodeRecord) {
        dao.insert(record)
    }

    fun delete(url: String) {
        dao.delete(url)
    }

}
