package io.horizontalsystems.bankwallet.modules.coin.tweets

import java.util.*

data class Tweet(
    val id: String,
    val user: TwitterUser,
    val text: String,
    val date: Date,
    val attachments: List<Attachment>,
    val referencedTweet: ReferencedTweetXxx?
) {
    sealed class Attachment {
        class Photo(val url: String) : Attachment()
        class Video(val previewImageUrl: String) : Attachment()
        class Poll(val options: List<Option>) : Attachment() {
            data class Option(val position: Int, val label: String, val votes: Int)
        }
    }

    enum class ReferenceType {
        Quoted, Retweeted, Replied
    }

    data class ReferencedTweetXxx(val referenceType: ReferenceType, val tweet: Tweet)
}
