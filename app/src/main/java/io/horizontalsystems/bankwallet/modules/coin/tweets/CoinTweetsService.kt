package io.horizontalsystems.bankwallet.modules.coin.tweets

import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.marketkit.models.LinkType
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await

class CoinTweetsService(
    private val coinUid: String,
    private val twitterProvider: TweetsProvider,
    private val marketKit: MarketKitWrapper,
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val disposables = CompositeDisposable()

    private val stateSubject = BehaviorSubject.create<DataState<List<Tweet>>>()
    val stateObservable: Observable<DataState<List<Tweet>>>
        get() = stateSubject

    val username: String? get() = user?.username
    private var user: TwitterUser? = null

    fun start() {
        fetch()
    }

    fun refresh() {
        fetch()
    }

    fun stop() {
        disposables.clear()
    }

    private fun fetch() {
        coroutineScope.launch {
            try {
                val tmpUser = user
                val twitterUser: TwitterUser

                if (tmpUser != null) {
                    twitterUser = tmpUser
                } else {
                    val marketInfoOverview = marketKit.marketInfoOverviewSingle(
                        coinUid,
                        "USD",
                        "en"
                    ).await()
                    val username = marketInfoOverview.links[LinkType.Twitter]
                    if (username.isNullOrBlank()) {
                        throw TweetsProvider.UserNotFound()
                    } else {
                        twitterUser = twitterProvider.userRequestSingle(username).await()
                        user = twitterUser
                    }
                }

                val tweets = twitterProvider.tweetsSingle(twitterUser).await()
                stateSubject.onNext(DataState.Success(tweets))
            } catch (e: Throwable) {
                stateSubject.onNext(DataState.Error(e))
            }
        }
    }
}


