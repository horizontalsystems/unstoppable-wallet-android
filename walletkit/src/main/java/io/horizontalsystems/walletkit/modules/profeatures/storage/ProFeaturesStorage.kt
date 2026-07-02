package io.horizontalsystems.walletkit.modules.profeatures.storage

import io.horizontalsystems.walletkit.core.storage.AppDatabase
import io.horizontalsystems.walletkit.modules.profeatures.ProNft

class ProFeaturesStorage(appDatabase: AppDatabase) {

    private val dao: ProFeaturesDao by lazy {
        appDatabase.proFeaturesDao()
    }

    fun add(sessionKey: ProFeaturesSessionKey) {
        dao.insert(sessionKey)
    }

    fun get(nftType: ProNft): ProFeaturesSessionKey? =
        dao.getOne(nftType.keyName)

    fun deleteAllExcept(accountIds: List<String>) {
        dao.deleteAllExcept(accountIds)
    }

    fun clear() {
        dao.clear()
    }

}
