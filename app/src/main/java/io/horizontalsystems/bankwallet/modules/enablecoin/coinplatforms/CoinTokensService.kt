package io.horizontalsystems.bankwallet.modules.enablecoin.coinplatforms

import io.horizontalsystems.bankwallet.core.supportedTokens
import io.horizontalsystems.xxxkit.models.Token
import io.reactivex.subjects.PublishSubject

class CoinTokensService {
    val approveTokensObservable = PublishSubject.create<CoinWithTokens>()
    val rejectApproveTokensObservable = PublishSubject.create<io.horizontalsystems.xxxkit.models.FullCoin>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveTokens(fullCoin: io.horizontalsystems.xxxkit.models.FullCoin, currentTokens: List<Token> = listOf()) {
        val supportedTokens = fullCoin.supportedTokens
        if (supportedTokens.size == 1) {
            approveTokensObservable.onNext(CoinWithTokens(fullCoin.coin, supportedTokens))
        } else {
            requestObservable.onNext(Request(fullCoin, currentTokens))
        }
    }

    fun select(tokens: List<Token>, coin: io.horizontalsystems.xxxkit.models.Coin) {
        approveTokensObservable.onNext(CoinWithTokens(coin, tokens))
    }

    fun cancel(fullCoin: io.horizontalsystems.xxxkit.models.FullCoin) {
        rejectApproveTokensObservable.onNext(fullCoin)
    }

    data class CoinWithTokens(
        val coin: io.horizontalsystems.xxxkit.models.Coin,
        val tokens: List<Token> = listOf()
    )

    data class Request(
        val fullCoin: io.horizontalsystems.xxxkit.models.FullCoin,
        val currentTokens: List<Token>
    )
}
