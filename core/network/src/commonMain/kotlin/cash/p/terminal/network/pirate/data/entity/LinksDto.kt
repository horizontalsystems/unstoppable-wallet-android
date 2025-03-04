package cash.p.terminal.network.pirate.data.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class LinksDto(
    val homepage: List<String>,
    @SerialName("blockchain_site")
    val blockchainSite: List<String>,
    @SerialName("official_forum_url")
    val officialForumUrl: String? = null,
    @SerialName("chat_url")
    val chatUrl: String? = null,
    @SerialName("announcement_url")
    val announcementUrl: String? = null,
    @SerialName("twitter_screen_name")
    val twitterScreenName: String? = null,
    @SerialName("facebook_username")
    val facebookUsername: String? = null,
    @SerialName("bitcointalk_identifier")
    val bitcointalkIdentifier: String? = null,
    @SerialName("telegram_channel_identifier")
    val telegramChannelIdentifier: String? = null,
    @SerialName("subreddit_url")
    val subredditUrl: String? = null,
)
