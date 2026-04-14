package com.quantum.wallet.bankwallet.core.storage

import com.quantum.wallet.bankwallet.entities.MoneroNodeRecord

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
