package io.horizontalsystems.bankwallet.modules.walletconnect.storage

import io.horizontalsystems.bankwallet.core.storage.AppDatabase
import io.horizontalsystems.bankwallet.modules.walletconnect.entity.WalletConnectSession

class WC1SessionStorage(appDatabase: AppDatabase) {

    private val dao: WC1SessionDao by lazy {
        appDatabase.wc1SessionDao()
    }

    fun getSessions(accountId: String, chainIds: List<Int>): List<WalletConnectSession> {
        return dao.getByAccountId(accountId, chainIds)
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
