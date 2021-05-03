package io.horizontalsystems.bankwallet.modules.market.posts

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateManager
import io.horizontalsystems.xrateskit.entities.CryptoNews
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

class MarketPostService(postManager: IRateManager) : Clearable {

    sealed class State {
        object Loaded : State()
        object Loading : State()
        class Failed(val error: Throwable) : State()
    }

    val stateObservable: BehaviorSubject<State> = BehaviorSubject.createDefault(State.Loading)

    var newsItems: List<CryptoNews> = listOf()

    private var disposable: Disposable? = null

    init {
        disposable = postManager.getCryptoNews()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    newsItems = it
                    stateObservable.onNext(State.Loaded)
                }, {
                    stateObservable.onError(it)
                })
    }

    override fun clear() {
        disposable?.dispose()
    }
}
