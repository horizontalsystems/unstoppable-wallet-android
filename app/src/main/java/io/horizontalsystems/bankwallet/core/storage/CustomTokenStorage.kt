package io.horizontalsystems.bankwallet.core.storage

import io.horizontalsystems.bankwallet.core.ICustomTokenStorage
import io.horizontalsystems.bankwallet.entities.CustomToken
import io.horizontalsystems.marketkit.models.CoinType

class CustomTokenStorage(private val appDatabase: AppDatabase) : ICustomTokenStorage {
    private val dao = appDatabase.customTokenDao()

    override fun customTokens(): List<CustomToken> {
        TODO("Not yet implemented")
    }

    override fun customTokens(filter: String): List<CustomToken> {
        TODO("Not yet implemented")
    }

    override fun customTokens(coinTypeIds: List<String>): List<CustomToken> {
        TODO("Not yet implemented")
    }

    override fun customToken(coinType: CoinType): CustomToken? {
        TODO("Not yet implemented")
    }

    override fun save(customTokens: List<CustomToken>) {
        TODO("Not yet implemented")
    }
}
