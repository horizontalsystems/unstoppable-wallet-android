package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CommunityDataDto(
    @SerialName("facebook_likes")
    val facebookLikes: Int? = null,
    @SerialName("twitter_followers")
    val twitterFollowers: Int? = null,
    @SerialName("reddit_average_posts_48h")
    val redditAveragePosts48h: Int? = null,
    @SerialName("reddit_average_comments_48h")
    val redditAverageComments48h: Int? = null,
    @SerialName("reddit_subscribers")
    val redditSubscribers: Int? = null,
    @SerialName("reddit_accounts_active_48h")
    val redditAccountsActive48h: Int? = null,
    @SerialName("telegram_channel_user_count")
    val telegramChannelUserCount: Int? = null
)
