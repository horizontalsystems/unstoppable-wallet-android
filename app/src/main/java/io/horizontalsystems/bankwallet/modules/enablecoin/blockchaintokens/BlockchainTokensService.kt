package io.horizontalsystems.bankwallet.modules.enablecoin.blockchaintokens

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import io.reactivex.subjects.PublishSubject

class BlockchainTokensService {
    val approveTokensObservable = PublishSubject.create<BlockchainWithTokens>()
    val rejectApproveTokensObservable = PublishSubject.create<Blockchain>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveTokens(blockchain: Blockchain, tokens: List<Token>, enabledTokens: List<Token>, allowEmpty: Boolean = false) {
        requestObservable.onNext(Request(blockchain, tokens, enabledTokens, allowEmpty))
    }

    fun select(tokens: List<Token>, blockchain: Blockchain) {
        approveTokensObservable.onNext(BlockchainWithTokens(blockchain, tokens))
    }

    fun cancel(blockchain: Blockchain) {
        rejectApproveTokensObservable.onNext(blockchain)
    }

    data class BlockchainWithTokens(val blockchain: Blockchain, val tokens: List<Token>)
    data class Request(val blockchain: Blockchain, val tokens: List<Token>, val enabledTokens: List<Token>, val allowEmpty: Boolean)
}
