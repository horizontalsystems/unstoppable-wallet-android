package cash.p.terminal.network.pirate.domain.enity

data class CommunityData(
    val facebookLikes: Int?,
    val twitterFollowers: Int?,
    val redditAveragePosts48h: Int?,
    val redditAverageComments48h: Int?,
    val redditSubscribers: Int?,
    val redditAccountsActive48h: Int?,
    val telegramChannelUserCount: Int?
)
