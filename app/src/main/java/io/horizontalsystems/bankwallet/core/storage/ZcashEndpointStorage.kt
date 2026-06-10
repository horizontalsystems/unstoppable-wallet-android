package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.ZcashEndpointRecord
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZcashEndpointStorage @Inject constructor(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.zcashEndpointDao() }

    fun getAll() = dao.getAll()

    fun save(record: ZcashEndpointRecord) {
        dao.insert(record)
    }

    fun delete(url: String) {
        dao.delete(url)
    }

}
