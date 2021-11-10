package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.twitter.twittertext.Extractor
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.DataState
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.core.helpers.DateHelper
import io.reactivex.disposables.CompositeDisposable

class CoinTweetsViewModel(
    private val service: CoinTweetsService,
    private val extractor: Extractor,
) : ViewModel() {
    val twitterPageUrl get() = "https://twitter.com/${service.username}"

    val isRefreshingLiveData = MutableLiveData<Boolean>(false)
    val itemsLiveData = MutableLiveData<List<TweetViewItem>>()
    val viewStateLiveData = MutableLiveData<ViewState>()

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
            val typeStringRes = when (referencedTweet.referenceType) {
                Tweet.ReferenceType.Quoted -> R.string.CoinPage_Twitter_Quoted
                Tweet.ReferenceType.Retweeted -> R.string.CoinPage_Twitter_Retweeted
                Tweet.ReferenceType.Replied -> R.string.CoinPage_Twitter_Replied
            }
            val title = TranslatableString.ResString(typeStringRes, "@${referencedTweet.tweet.user.username}")

            ReferencedTweetViewItem(
                title = title,
                text = referencedTweet.tweet.text
            )
        },
        entities = extractor.extractEntitiesWithIndices(tweet.text),
        url = "https://twitter.com/${tweet.user.username}/status/${tweet.id}"
    )

    fun refresh() {
        isRefreshingLiveData.postValue(true)
        service.refresh()
    }

    override fun onCleared() {
        service.stop()
        disposables.clear()
    }
}

