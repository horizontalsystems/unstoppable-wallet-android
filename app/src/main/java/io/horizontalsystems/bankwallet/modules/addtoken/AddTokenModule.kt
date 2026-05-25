package io.horizontalsystems.bankwallet.modules.addtoken

import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery

object AddTokenModule {

    interface IAddTokenBlockchainService {
        fun isValid(reference: String): Boolean
        fun tokenQuery(reference: String): TokenQuery
        suspend fun token(reference: String): Token
    }
}
