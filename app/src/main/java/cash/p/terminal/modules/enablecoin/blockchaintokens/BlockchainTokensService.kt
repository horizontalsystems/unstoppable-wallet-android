package cash.p.terminal.modules.enablecoin.blockchaintokens

import io.horizontalsystems.core.entities.Blockchain
import io.reactivex.subjects.PublishSubject

class BlockchainTokensService {
    val approveTokensObservable = PublishSubject.create<BlockchainWithTokens>()
    val rejectApproveTokensObservable = PublishSubject.create<Blockchain>()
    val requestObservable = PublishSubject.create<Request>()

    fun approveTokens(blockchain: Blockchain, tokens: List<cash.p.terminal.wallet.Token>, enabledTokens: List<cash.p.terminal.wallet.Token>, allowEmpty: Boolean = false) {
        requestObservable.onNext(Request(blockchain, tokens, enabledTokens, allowEmpty))
    }

    fun select(tokens: List<cash.p.terminal.wallet.Token>, blockchain: Blockchain) {
        approveTokensObservable.onNext(BlockchainWithTokens(blockchain, tokens))
    }

    fun cancel(blockchain: Blockchain) {
        rejectApproveTokensObservable.onNext(blockchain)
    }

    data class BlockchainWithTokens(val blockchain: Blockchain, val tokens: List<cash.p.terminal.wallet.Token>)
    data class Request(val blockchain: Blockchain, val tokens: List<cash.p.terminal.wallet.Token>, val enabledTokens: List<cash.p.terminal.wallet.Token>, val allowEmpty: Boolean)
}
