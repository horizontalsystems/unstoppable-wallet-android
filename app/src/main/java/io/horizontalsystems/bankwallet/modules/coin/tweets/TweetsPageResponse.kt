package io.horizontalsystems.bankwallet.modules.coin.tweets

import com.google.gson.annotations.SerializedName
import java.util.*

data class TweetsPageResponse(
    val data: List<RawTweet>,
    val includes: Includes,
) {
    data class Includes(
        @SerializedName("media")
        val media: List<Media>,
        @SerializedName("polls")
        val polls: List<Poll>,
        @SerializedName("users")
        val users: List<TwitterUser>,
        @SerializedName("tweets")
        val referencedTweets: List<RawTweet>
    )

    fun tweets(user: TwitterUser): List<Tweet> {
        return data.map { rawTweet ->
            val attachments = mutableListOf<Tweet.Attachment>()
            rawTweet.attachments?.mediaKeys?.let { mediaKeys ->
                for (mediaKey in mediaKeys) {
                    val media = includes.media.find { singleMedia -> singleMedia.key == mediaKey }
                    if (media != null) {
                        when (media.type) {
                            "photo" -> {
                                media.url?.let {
                                    attachments.add(Tweet.Attachment.Photo(it))
                                }
                            }
                            "video" -> {
                                media.previewImageUrl?.let {
                                    attachments.add(Tweet.Attachment.Video(it))
                                }
                            }
                        }
                    }
                }
            }
            rawTweet.attachments?.pollIds?.let { pollIds ->
                for (pollId in pollIds) {
                    val poll = includes.polls.find { it.id == pollId }
                    if (poll != null) {
                        val options = poll.options.map { Tweet.Attachment.Poll.Option(it.position, it.label, it.votes) }
                        attachments.add(Tweet.Attachment.Poll(options))
                    }
                }
            }

            var referencedTweet: Tweet.ReferencedTweetXxx? = null
            rawTweet.referencedTweets?.firstOrNull()?.let { tweetReference ->
                includes.referencedTweets.find { tweet -> tweet.id == tweetReference.id }?.let { rawReferencedTweet ->
                    includes.users.find { user -> user.id == rawReferencedTweet.authorId }?.let { referencedTweetAuthor ->
                        val tweet = Tweet(
                            rawReferencedTweet.id,
                            referencedTweetAuthor,
                            rawReferencedTweet.text,
                            rawReferencedTweet.date,
                            listOf(),
                            null
                        )

                        referencedTweet = when (tweetReference.type) {
                            "quoted" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Quoted, tweet)
                            "retweeted" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Retweeted, tweet)
                            "replied_to" -> Tweet.ReferencedTweetXxx(Tweet.ReferenceType.Replied, tweet)
                            else -> null
                        }
                    }
                }
            }

            Tweet(
                rawTweet.id,
                user,
                rawTweet.text,
                rawTweet.date,
                attachments,
                referencedTweet
            )
        }
    }

    data class RawTweet(
        val id: String,
        @SerializedName("created_at")
        val date: Date,
        @SerializedName("author_id")
        val authorId: String,
        val text: String,
        val attachments: Attachments?,
        @SerializedName("referenced_tweets")
        val referencedTweets: List<ReferencedTweet>?
    ) {
        data class Attachments(
            @SerializedName("media_keys")
            val mediaKeys: List<String>?,
            @SerializedName("poll_ids")
            val pollIds: List<String>?,
        )
    }

    data class Media(
        @SerializedName("media_key")
        val key: String,
        val type: String,
        val url: String?,
        @SerializedName("preview_image_url")
        val previewImageUrl: String?,
    )

    data class Poll(
        val id: String,
        val options: List<PollOption>,
    )

    data class PollOption(
        val position: Int,
        val label: String,
        val votes: Int,
    )

    data class ReferencedTweet(
        val type: String,
        val id: String,
    )

//    static var utcDateFormatter: DateFormatter {
//        let dateFormatter = DateFormatter()
//        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
//        dateFormatter.timeZone = TimeZone(abbreviation: "GMT")!
//        dateFormatter.locale = Locale(identifier: "en_US_POSIX")
//        return dateFormatter
//    }
//
//    static let utcDateTransform: TransformOf<Date, String> = TransformOf(fromJSON: { string -> Date? in
//        guard let string = string else { return nil }
//        return utcDateFormatter.date(from: string)
//    }, toJSON: { (value: Date?) in
//        guard let value = value else { return nil }
//        return utcDateFormatter.string(from: value)
//    })

}
