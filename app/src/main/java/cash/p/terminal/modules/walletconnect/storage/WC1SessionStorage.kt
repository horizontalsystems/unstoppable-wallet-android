package cash.p.terminal.modules.walletconnect.storage

import cash.p.terminal.core.storage.AppDatabase
import cash.p.terminal.modules.walletconnect.entity.WalletConnectSession

class WC1SessionStorage(appDatabase: AppDatabase) {

    private val dao: WC1SessionDao by lazy {
        appDatabase.wc1SessionDao()
    }

    fun getSessions(accountId: String): List<WalletConnectSession> {
        return dao.getByAccountId(accountId)
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
