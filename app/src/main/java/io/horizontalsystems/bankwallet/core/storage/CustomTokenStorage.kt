package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICustomTokenStorage
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.models.CoinType

class CustomTokenStorage(appDatabase: AppDatabase) : ICustomTokenStorage {
    private val dao = appDatabase.customTokenDao()

    override fun customTokens(): List<CustomToken> {
        return dao.getCustomTokens()
    }

    override fun customTokens(filter: String): List<CustomToken> {
        return dao.getCustomTokens("%$filter%")
    }

    override fun customTokens(coinTypeIds: List<String>): List<CustomToken> {
        return dao.getCustomTokens(coinTypeIds)
    }

    override fun customToken(coinType: CoinType): CustomToken? {
        return dao.getCustomToken(coinType)
    }

    override fun save(customTokens: List<CustomToken>) {
        dao.insert(customTokens)
    }
}
