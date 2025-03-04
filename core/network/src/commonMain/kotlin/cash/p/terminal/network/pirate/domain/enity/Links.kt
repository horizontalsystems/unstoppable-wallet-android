package cash.p.terminal.network.pirate.domain.enity

data class Links(
    val homepage: List<String>,
    val blockchainSite: List<String>,
    val officialForumUrl: String?,
    val chatUrl: String?,
    val announcementUrl: String?,
    val twitterScreenName: String?,
    val facebookUsername: String?,
    val bitcointalkIdentifier: String?,
    val telegramChannelIdentifier: String?,
    val subredditUrl: String?,
)
