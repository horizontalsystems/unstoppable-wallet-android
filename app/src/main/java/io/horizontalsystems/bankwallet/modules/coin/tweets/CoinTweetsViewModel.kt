package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.twitter.twittertext.Extractor
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.modules.market.tvl.TvlModule
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.disposables.CompositeDisposable

class CoinTweetsViewModel(
    private val service: CoinTweetsService,
    private val extractor: Extractor,
) : ViewModel() {
    val twitterPageUrl get() = "https://twitter.com/${service.username}"

    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val itemsLiveData = MutableLiveData<List<TweetViewItem>>()
    val viewStateLiveData = MutableLiveData<TvlModule.ViewState>()

    private val disposables = CompositeDisposable()

    init {
        service.stateObservable
            .subscribeIO { state ->
                isRefreshingLiveData.postValue(state == DataState.Loading)

                state.dataOrNull?.let {
                    itemsLiveData.postValue(it.map { getTweetViewItem(it) })
                }

                state.viewState?.let {
                    viewStateLiveData.postValue(it)
                }
            }
            .let {
                disposables.add(it)
            }

        service.start()
    }

    private fun getTweetViewItem(tweet: Tweet) = TweetViewItem(
        title = tweet.user.name,
        subtitle = "@${tweet.user.username}",
        titleImageUrl = tweet.user.profileImageUrl,
        text = tweet.text,
        attachments = tweet.attachments,
        date = DateHelper.getDayAndTime(tweet.date),
        referencedTweet = tweet.referencedTweet?.let { referencedTweet ->
            val typeString = when (referencedTweet.referenceType) {
                Tweet.ReferenceType.Quoted -> "Quoted"
                Tweet.ReferenceType.Retweeted -> "Retweeted"
                Tweet.ReferenceType.Replied -> "Replying to"
            }
            val referencedTweetTitle = "$typeString @${referencedTweet.tweet.user.username}"

            ReferencedTweetViewItem(
                title = referencedTweetTitle,
                text = referencedTweet.tweet.text
            )
        },
        entities = extractor.extractEntitiesWithIndices(tweet.text),
        url = "https://twitter.com/${tweet.user.username}/status/${tweet.id}"
    )

    val DataState<*>.viewState: TvlModule.ViewState?
        get() = when (this) {
            is DataState.Error -> TvlModule.ViewState.Error
            is DataState.Success -> TvlModule.ViewState.Success
            else -> null
        }

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}

