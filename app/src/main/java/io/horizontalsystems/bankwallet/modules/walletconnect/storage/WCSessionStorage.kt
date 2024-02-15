package io.horizontalsystems.bankwallet.modules.walletconnect.storage

import io.horizontalsystems.bankwallet.core.storage.AppDatabase

class WCSessionStorage(appDatabase: AppDatabase) {

    private val dao: WCSessionDao by lazy {
        appDatabase.wcSessionDao()
    }

    fun getAllSessions(): List<WalletConnectV2Session> {
        return dao.getAll()
    }

    fun getSessionsByAccountId(accountId: String): List<WalletConnectV2Session> {
        return dao.getByAccountId(accountId)
    }

    fun save(sessions: List<WalletConnectV2Session>) {
        dao.insert(sessions)
    }

    fun deleteSessionsExcept(accountIds: List<String> = listOf()) {
        dao.deleteAllExcept(accountIds)
    }

    fun deleteSessionsByTopics(topics: List<String> = listOf()) {
        dao.deleteByTopics(topics)
    }

}
