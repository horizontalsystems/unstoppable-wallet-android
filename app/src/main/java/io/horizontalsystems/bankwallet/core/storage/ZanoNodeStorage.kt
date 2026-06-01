package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.ZanoNodeRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZanoNodeStorage @Inject constructor(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.zanoNodeDao() }

    fun getAll() = dao.getAll()

    fun save(record: ZanoNodeRecord) {
        dao.insert(record)
    }

    fun delete(url: String) {
        dao.delete(url)
    }

}
