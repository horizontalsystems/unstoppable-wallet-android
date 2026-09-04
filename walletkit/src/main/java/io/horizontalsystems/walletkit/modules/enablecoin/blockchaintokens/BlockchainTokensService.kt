package io.horizontalsystems.walletkit.modules.enablecoin.blockchaintokens

import io.horizontalsystems.marketkit.models.Blockchain
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class BlockchainTokensService {
    // Bounded at 64 with newest-wins overflow: these events are user-driven and sparse
    // (at most a handful per screen session), so the buffer is effectively lossless while
    // still capping memory if a collector ever stalls.
    private val _approveTokensFlow = MutableSharedFlow<BlockchainWithTokens>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val approveTokensFlow: Flow<BlockchainWithTokens> = _approveTokensFlow

    private val _rejectApproveTokensFlow = MutableSharedFlow<Blockchain>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val rejectApproveTokensFlow: Flow<Blockchain> = _rejectApproveTokensFlow

    private val _requestFlow = MutableSharedFlow<Request>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
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
