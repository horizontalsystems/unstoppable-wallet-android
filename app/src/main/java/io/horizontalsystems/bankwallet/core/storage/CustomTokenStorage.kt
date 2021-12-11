package io.horizontalsystems.bankwallet.core.storage

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.bankwallet.core.ICustomTokenStorage
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformType

class CustomTokenStorage(appDatabase: AppDatabase) : ICustomTokenStorage {
    private val dao = appDatabase.customTokenDao()

    override fun customTokens(platformType: PlatformType, filter: String): List<CustomToken> {
        val platformCondition =
            platformType.coinTypeIdPrefixes.joinToString(" OR ") { "coinType LIKE '$it%'" }

        val query =
            """
                SELECT * FROM CustomToken 
                WHERE (coinName LIKE '%$filter%' OR coinCode LIKE '%$filter%')
                AND ($platformCondition)
                """
        return dao.getCustomTokens(SimpleSQLiteQuery(query))
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
