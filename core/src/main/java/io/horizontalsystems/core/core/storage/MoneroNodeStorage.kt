package io.horizontalsystems.core.core.storage

import io.horizontalsystems.core.entities.MoneroNodeRecord

class MoneroNodeStorage(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.moneroNodeDao() }

    fun getAll() = dao.getAll()

    fun save(record: MoneroNodeRecord) {
        dao.insert(record)
    }

    fun delete(url: String) {
        dao.delete(url)
    }

}
