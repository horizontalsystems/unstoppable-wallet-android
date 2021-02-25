package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.entities.WalletConnectSession

class WalletConnectSessionStorage(appDatabase: AppDatabase) {

    private val dao: WalletConnectSessionDao by lazy {
        appDatabase.walletConnectSessionDao()
    }

    fun getSessions(): List<WalletConnectSession> {
        return dao.getAll()
    }

    fun save(session: WalletConnectSession) {
        dao.insert(session)
    }

    fun deleteSessionsByPeerId(peerId: String) {
        dao.deleteByPeerId(peerId)
    }

    fun deleteSessionsExcept(accountIds: List<String> = listOf()) {
        dao.deleteAllExcept(accountIds)
    }

}
