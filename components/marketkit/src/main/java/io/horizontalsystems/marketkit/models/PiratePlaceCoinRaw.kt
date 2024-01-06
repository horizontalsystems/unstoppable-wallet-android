package io.horizontalsystems.marketkit.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

class PiratePlaceCoinRaw (
    val rank: Int?,
    val id: String,
    val name: String,
    val symbol: String,
    @SerializedName("circulating_supply")
    val circulatingSupply: BigDecimal,
    @SerializedName("total_supply")
    val totalSupply: Long,
    @SerializedName("max_supply")
    val maxSupply: Long?,
    val changes: Changes,
    @SerializedName("market_cap")
    val marketCap: MarketCap,
    val image: String,
    val price: Price,
    val categories: List<Category>,
    val description: Description,
    val links: Links,
    @SerializedName("fully_diluted_valuation")
    val fullyDilutedValuation: FullyDilutedValuation,
    val ath: Ath,
    @SerializedName("ath_percentage")
    val athPercentage: AthPercentage,
    val high24h: HighLow,
    val low24h: HighLow,
    @SerializedName("community_data")
    val communityData: CommunityData,
    val graphs: Graphs,
    @SerializedName("is_active")
    val isActive: Boolean,
    @SerializedName("is_currency")
    val isCurrency: Boolean,
    @SerializedName("is_real_currency")
    val isRealCurrency: Boolean,
    @SerializedName("updated_at")
    val updatedAt: String
)

data class Changes(
        val price: PriceChange
)

data class PriceChange(
        @SerializedName("percentage_1h")
        val percentage1h: Map<String, BigDecimal>,
        @SerializedName("percentage_24h")
        val percentage24h: Map<String, BigDecimal>,
        @SerializedName("percentage_7d")
        val percentage7d: Map<String, BigDecimal>,
        @SerializedName("percentage_30d")
        val percentage30d: Map<String, BigDecimal>,
        @SerializedName("percentage_1y")
        val percentage1y: Map<String, BigDecimal>
)

data class MarketCap(
        @SerializedName("value_24h")
        val value24h: Map<String, BigDecimal>
)

data class Price(
        val aud: BigDecimal,
        val brl: BigDecimal,
        val btc: BigDecimal,
        val cad: BigDecimal,
        val chf: BigDecimal,
        val cny: BigDecimal,
        val eth: BigDecimal,
        val eur: BigDecimal,
        val gbp: BigDecimal,
        val hkd: BigDecimal,
        val huf: BigDecimal,
        val ils: BigDecimal,
        val inr: BigDecimal,
        val noc: BigDecimal,
        val php: BigDecimal,
        val jpy: BigDecimal,
        val pln: BigDecimal,
        val rub: BigDecimal,
        val sgd: BigDecimal,
        val uah: BigDecimal,
        val usd: BigDecimal,
        val zar: BigDecimal
)

data class Category(
        val name: String
)

data class Description(
        val en: String,
        val ru: String
)

data class Links(
        val homepage: List<String>,
        @SerializedName("blockchain_site")
        val blockchainSite: List<String>,
        @SerializedName("repos_url")
        val reposUrl: ReposUrl
)

data class FullyDilutedValuation(
        val aud: BigDecimal,
        val ars: BigDecimal,
        val brl: BigDecimal,
        val btc: BigDecimal,
        val cad: BigDecimal,
        val chf: BigDecimal,
        val cny: BigDecimal,
        val eth: BigDecimal,
        val eur: BigDecimal,
        val gbp: BigDecimal,
        val hkd: BigDecimal,
        val huf: BigDecimal,
        val ils: BigDecimal,
        val inr: BigDecimal,
        val noc: BigDecimal,
        val php: BigDecimal,
        val jpy: BigDecimal,
        val pln: BigDecimal,
        val rub: BigDecimal,
        val sgd: BigDecimal,
        val uah: BigDecimal,
        val usd: BigDecimal,
        val zar: BigDecimal
)

data class Ath(
        val aud: BigDecimal,
        val ars: BigDecimal,
        val brl: BigDecimal,
        val btc: BigDecimal,
        val cad: BigDecimal,
        val chf: BigDecimal,
        val cny: BigDecimal,
        val eth: BigDecimal,
        val eur: BigDecimal,
        val gbp: BigDecimal,
        val hkd: BigDecimal,
        val huf: BigDecimal,
        val ils: BigDecimal,
        val inr: BigDecimal,
        val noc: BigDecimal,
        val php: BigDecimal,
        val jpy: BigDecimal,
        val pln: BigDecimal,
        val rub: BigDecimal,
        val sgd: BigDecimal,
        val uah: BigDecimal,
        val usd: BigDecimal,
        val zar: BigDecimal
)

data class AthPercentage(
        val aud: BigDecimal,
        val ars: BigDecimal,
        val brl: BigDecimal,
        val btc: BigDecimal,
        val cad: BigDecimal,
        val chf: BigDecimal,
        val cny: BigDecimal,
        val eth: BigDecimal,
        val eur: BigDecimal,
        val gbp: BigDecimal,
        val hkd: BigDecimal,
        val huf: BigDecimal,
        val ils: BigDecimal,
        val inr: BigDecimal,
        val noc: BigDecimal,
        val php: BigDecimal,
        val jpy: BigDecimal,
        val pln: BigDecimal,
        val rub: BigDecimal,
        val sgd: BigDecimal,
        val uah: BigDecimal,
        val usd: BigDecimal,
        val zar: BigDecimal
)

data class HighLow(
        val aud: BigDecimal,
        val ars: BigDecimal,
        val brl: BigDecimal,
        val btc: BigDecimal,
        val cad: BigDecimal,
        val chf: BigDecimal,
        val cny: BigDecimal,
        val eth: BigDecimal,
        val eur: BigDecimal,
        val gbp: BigDecimal,
        val hkd: BigDecimal,
        val huf: BigDecimal,
        val ils: BigDecimal,
        val inr: BigDecimal,
        val noc: BigDecimal,
        val php: BigDecimal,
        val jpy: BigDecimal,
        val pln: BigDecimal,
        val rub: BigDecimal,
        val sgd: BigDecimal,
        val uah: BigDecimal,
        val usd: BigDecimal,
        val zar: BigDecimal
)

data class CommunityData(
        @SerializedName("facebook_likes")
        val facebookLikes: Int?,
        @SerializedName("twitter_followers")
        val twitterFollowers: Int,
        @SerializedName("reddit_average_posts_48h")
        val redditAveragePosts48h: Int,
        @SerializedName("reddit_average_comments_48h")
        val redditAverageComments48h: Int,
        @SerializedName("reddit_subscribers")
        val redditSubscribers: Int,
        @SerializedName("reddit_accounts_active_48h")
        val redditAccountsActive48h: Int,
        @SerializedName("telegram_channel_user_count")
        val telegramChannelUserCount: Int
)

data class Graphs(
        val aud: GraphUrls,
        val ars: GraphUrls,
        val brl: GraphUrls,
        val btc: GraphUrls,
        val cad: GraphUrls,
        val chf: GraphUrls,
        val cny: GraphUrls,
        val eth: GraphUrls,
        val eur: GraphUrls,
        val gbp: GraphUrls,
        val hkd: GraphUrls,
        val huf: GraphUrls,
        val ils: GraphUrls,
        val inr: GraphUrls,
        val noc: GraphUrls,
        val php: GraphUrls,
        val jpy: GraphUrls,
        val pln: GraphUrls,
        val rub: GraphUrls,
        val sgd: GraphUrls,
        val uah: GraphUrls,
        val usd: GraphUrls,
        val zar: GraphUrls
)

data class GraphUrls(
        val day: String,
        val hour: String,
        val max: String,
        val month: String,
        val week: String,
        val year: String
)

data class ReposUrl(
        val github: List<String>,
        val bitbucket: List<String>?
)
