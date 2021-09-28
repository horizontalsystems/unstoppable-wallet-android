package io.horizontalsystems.bankwallet.modules.market.posts

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.bankwallet.core.managers.CryptoNews
import io.horizontalsystems.core.BackgroundManager
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketPostService(
        private val postManager: IRateManager,
        private val backgroundManager: BackgroundManager,
) : Clearable, BackgroundManager.Listener {

    sealed class State {
        object Loaded : State()
        object Loading : State()
        class Failed(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    var newsItems: List<CryptoNews> = listOf()

    private var disposable: Disposable? = null

    init {
        backgroundManager.registerListener(this)
        fetchPosts()
    }

    private fun fetchPosts() {
        disposable?.dispose()
        disposable = postManager.getCryptoNews()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    newsItems = it
                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onNext(State.Failed(it))
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
