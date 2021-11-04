package io.horizontalsystems.bankwallet.modules.coin.tweets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.twitter.twittertext.Extractor
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.marketkit.models.FullCoin

object CoinTweetsModule {
    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = CoinTweetsService(fullCoin.coin.uid, TweetsProvider("AAAAAAAAAAAAAAAAAAAAAJgeNwEAAAAA6xVpR6xLKTrxIA3kkSyRA92LDpA%3Da6auybDwcymUyh2BcS6zZwicUdxGtrzJC0qvOSdRwKLeqBGhwB"), App.marketKit)

            return CoinTweetsViewModel(service, Extractor()) as T
        }
    }
}

data class TweetViewItem(
    val title: String,
    val subtitle: String,
    val titleImageUrl: String,
    val text: String,
    val attachments: List<Tweet.Attachment>,
    val date: String,
    val referencedTweet: ReferencedTweetViewItem?,
    val entities: List<Extractor.Entity>,
    val url: String,
)

data class ReferencedTweetViewItem(val title: String, val text: String)