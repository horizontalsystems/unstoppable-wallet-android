package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BaseTokenManager(
    private val coinManager: ICoinManager,
    private val localStorage: ILocalStorage,
) {
    val tokens by lazy {
        listOf(
            TokenQuery(BlockchainType.Bitcoin, TokenType.Derived(TokenType.Derivation.Bip84)),
            TokenQuery(BlockchainType.Ethereum, TokenType.Native),
        ).mapNotNull {
            coinManager.getToken(it)
        }
    }

    var token = localStorage.balanceTotalCoinUid?.let { balanceTotalCoinUid ->
        tokens.find { it.coin.uid == balanceTotalCoinUid }
    } ?: tokens.firstOrNull()
        private set

    private val _baseTokenFlow = MutableStateFlow(token)
    val baseTokenFlow = _baseTokenFlow.asStateFlow()

    fun toggleBaseToken() {
        val indexOfNext = tokens.indexOf(token) + 1
        setBaseToken(tokens.getOrNull(indexOfNext) ?: tokens.firstOrNull())
    }

    fun setBaseTokenQueryId(tokenQueryId: String) {
        val token = TokenQuery.fromId(tokenQueryId)?.let { coinManager.getToken(it) } ?: tokens.first()

        setBaseToken(token)
    }

    private fun setBaseToken(token: Token?) {
        this.token = token
        localStorage.balanceTotalCoinUid = token?.coin?.uid

        _baseTokenFlow.update {
            token
        }
    }

}
