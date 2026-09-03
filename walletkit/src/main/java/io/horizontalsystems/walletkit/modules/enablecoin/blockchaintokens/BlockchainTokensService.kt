package io.horizontalsystems.walletkit.modules.enablecoin.blockchaintokens

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BlockchainTokensService {
    private val _approveTokensFlow = MutableSharedFlow<BlockchainWithTokens>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val approveTokensFlow: Flow<BlockchainWithTokens> = _approveTokensFlow

    private val _rejectApproveTokensFlow = MutableSharedFlow<Blockchain>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rejectApproveTokensFlow: Flow<Blockchain> = _rejectApproveTokensFlow

    private val _requestFlow = MutableSharedFlow<Request>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val requestFlow: Flow<Request> = _requestFlow

    fun approveTokens(blockchain: Blockchain, tokens: List<Token>, enabledTokens: List<Token>, allowEmpty: Boolean = false) {
        requestFlow.tryEmit(Request(blockchain, tokens, enabledTokens, allowEmpty))
    }

    fun select(tokens: List<Token>, blockchain: Blockchain) {
        approveTokensFlow.tryEmit(BlockchainWithTokens(blockchain, tokens))
    }

    fun cancel(blockchain: Blockchain) {
        rejectApproveTokensFlow.tryEmit(blockchain)
    }

    data class BlockchainWithTokens(val blockchain: Blockchain, val tokens: List<Token>)
    data class Request(val blockchain: Blockchain, val tokens: List<Token>, val enabledTokens: List<Token>, val allowEmpty: Boolean)
}
