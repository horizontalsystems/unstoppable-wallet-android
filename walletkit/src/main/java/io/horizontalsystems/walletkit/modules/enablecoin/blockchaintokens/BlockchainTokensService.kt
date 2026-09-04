package io.horizontalsystems.walletkit.modules.enablecoin.blockchaintokens

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BlockchainTokensService {
    private val _approveTokensFlow = MutableSharedFlow<BlockchainWithTokens>(extraBufferCapacity = Int.MAX_VALUE)
    val approveTokensFlow: Flow<BlockchainWithTokens> = _approveTokensFlow

    private val _rejectApproveTokensFlow = MutableSharedFlow<Blockchain>(extraBufferCapacity = Int.MAX_VALUE)
    val rejectApproveTokensFlow: Flow<Blockchain> = _rejectApproveTokensFlow

    private val _requestFlow = MutableSharedFlow<Request>(extraBufferCapacity = Int.MAX_VALUE)
    val requestFlow: Flow<Request> = _requestFlow

    fun approveTokens(blockchain: Blockchain, tokens: List<Token>, enabledTokens: List<Token>, allowEmpty: Boolean = false) {
        _requestFlow.tryEmit(Request(blockchain, tokens, enabledTokens, allowEmpty))
    }

    fun select(tokens: List<Token>, blockchain: Blockchain) {
        _approveTokensFlow.tryEmit(BlockchainWithTokens(blockchain, tokens))
    }

    fun cancel(blockchain: Blockchain) {
        _rejectApproveTokensFlow.tryEmit(blockchain)
    }

    data class BlockchainWithTokens(val blockchain: Blockchain, val tokens: List<Token>)
    data class Request(val blockchain: Blockchain, val tokens: List<Token>, val enabledTokens: List<Token>, val allowEmpty: Boolean)
}
