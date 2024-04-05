package io.horizontalsystems.bankwallet.modules.market.posts

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.models.Post
import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class MarketPostService(
    private val marketKit: MarketKitWrapper,
    private val backgroundManager: BackgroundManager,
) : BackgroundManager.Listener {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var xxxJob: Job? = null

    private val stateSubject = BehaviorSubject.create<DataState<List<Post>>>()
    val stateObservable: Observable<DataState<List<Post>>>
        get() = stateSubject

    init {
        backgroundManager.registerListener(this)
        fetchPosts()
    }

    private fun fetchPosts() {
        xxxJob?.cancel()
        xxxJob = coroutineScope.launch {
            try {
                val posts = marketKit.postsSingle().await()
                stateSubject.onNext(DataState.Success(posts))
            } catch (e: Throwable) {
                stateSubject.onNext(DataState.Error(e))
            }
        }
    }

    override fun willEnterForeground() {
        fetchPosts()
    }

    fun start() {
        fetchPosts()
    }

    fun stop() {
        coroutineScope.cancel()
        backgroundManager.unregisterListener(this)
    }

    fun refresh() {
        fetchPosts()
    }
}
