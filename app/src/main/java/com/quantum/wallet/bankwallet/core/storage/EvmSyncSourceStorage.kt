package com.quantum.wallet.bankwallet.core.storage

import com.quantum.wallet.bankwallet.entities.EvmSyncSourceRecord
import io.horizontalsystems.marketkit.models.BlockchainType

class EvmSyncSourceStorage(appDatabase: AppDatabase) {

    private val dao by lazy { appDatabase.evmSyncSourceDao() }

    fun evmSyncSources(blockchainType: BlockchainType): List<EvmSyncSourceRecord> {
        return dao.getEvmSyncSources(blockchainType.uid)
    }

    fun getAll() = dao.getAll()

    fun save(evmSyncSourceRecord: EvmSyncSourceRecord) {
        dao.insert(evmSyncSourceRecord)
    }

    fun delete(blockchainTypeUid: String, url: String) {
        dao.delete(blockchainTypeUid, url)
    }

}
