package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import io.horizontalsystems.marketkit.models.Token
import io.reactivex.subjects.PublishSubject

class CoinTokensService {
    val approveTokensObservable = PublishSubject.create<CoinWithTokens>()
    val rejectApproveTokensObservable = PublishSubject.create<io.horizontalsystems.marketkit.models.FullCoin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveTokens(fullCoin: io.horizontalsystems.marketkit.models.FullCoin, currentTokens: List<Token> = listOf(), allowEmpty: Boolean = false) {
        requestObservable.onNext(Request(fullCoin, currentTokens, allowEmpty))
    }

    fun select(tokens: List<Token>, coin: io.horizontalsystems.marketkit.models.Coin) {
        approveTokensObservable.onNext(CoinWithTokens(coin, tokens))
    }

    fun cancel(fullCoin: io.horizontalsystems.marketkit.models.FullCoin) {
        rejectApproveTokensObservable.onNext(fullCoin)
    }

    data class CoinWithTokens(
        val coin: io.horizontalsystems.marketkit.models.Coin,
        val tokens: List<Token> = listOf()
    )

    data class Request(
        val fullCoin: io.horizontalsystems.marketkit.models.FullCoin,
        val currentTokens: List<Token>,
        val allowEmpty: Boolean
    )
}
