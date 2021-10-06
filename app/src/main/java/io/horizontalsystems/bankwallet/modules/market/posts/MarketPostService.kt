package io.horizontalsystems.bankwallet.modules.market.posts

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Post
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketPostService(
    private val marketKit: MarketKit,
    private val backgroundManager: BackgroundManager,
) : Clearable, BackgroundManager.Listener {

    sealed class State {
        object Loading : State()
        class Loaded(val posts: List<Post>) : State()
        class Failed(val error: Throwable) : State()
    }

    private var disposable: Disposable? = null

    var state: State = State.Loading
        private set(value) {
            field = value
            stateObservable.onNext(value)
        }
    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    init {
        backgroundManager.registerListener(this)
        fetchPosts()
    }

    private fun fetchPosts() {
        state = State.Loading

        disposable?.dispose()
        disposable = marketKit.postsSingle()
            .subscribeOn(Schedulers.io())
            .subscribe({
                state = State.Loaded(it)
            }, {
                state = State.Failed(it)
            })
    }

    override fun clear() {
        disposable?.dispose()
        backgroundManager.unregisterListener(this)
    }

    override fun willEnterForeground() {
        fetchPosts()
    }

    fun refresh() {
        fetchPosts()
    }
}
