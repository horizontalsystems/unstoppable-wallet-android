package io.horizontalsystems.walletkit.modules.market.posts

import io.horizontalsystems.walletkit.core.BackgroundManager
import io.horizontalsystems.walletkit.core.BackgroundManagerState
import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.entities.DataState
import io.horizontalsystems.marketkit.models.Post
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class MarketPostService(
    private val marketKit: MarketKitWrapper,
    private val backgroundManager: BackgroundManager,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    private val _stateFlow = MutableSharedFlow<DataState<List<Post>>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val stateFlow: Flow<DataState<List<Post>>>
        get() = _stateFlow

    private fun fetchPosts() {
        job?.cancel()
        job = coroutineScope.launch {
            try {
                val posts = marketKit.postsSingle()
                _stateFlow.tryEmit(DataState.Success(posts))
            } catch (e: Throwable) {
                _stateFlow.tryEmit(DataState.Error(e))
            }
        }
    }

    fun start() {
        fetchPosts()
        coroutineScope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterForeground) {
                    fetchPosts()
                }
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }

    fun refresh() {
        fetchPosts()
    }
}
