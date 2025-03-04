package cash.p.terminal.wallet.providers.mapper

import cash.p.terminal.network.pirate.domain.enity.Changes
import cash.p.terminal.network.pirate.domain.enity.Links
import cash.p.terminal.network.pirate.domain.enity.PiratePlaceCoin
import cash.p.terminal.wallet.models.MarketInfoOverviewRaw
import java.math.BigDecimal
import java.util.Locale

internal class PirateCoinInfoMapper {

    fun mapCoinInfo(piratePlaceCoin: PiratePlaceCoin) =
        MarketInfoOverviewRaw(
            performance = mapOf(
                mapPerformance("usd", piratePlaceCoin.changes),
                mapPerformance("btc", piratePlaceCoin.changes),
                mapPerformance("eth", piratePlaceCoin.changes),
            ),
            genesisDate = null,
            categories = emptyList(),
            description = piratePlaceCoin.description[Locale.getDefault().language]
                ?: piratePlaceCoin.description["en"],
            links = mapLinks(piratePlaceCoin.links),
            marketData = MarketInfoOverviewRaw.MarketData(
                marketCap = piratePlaceCoin.marketCap["usd"],
                marketCapRank = piratePlaceCoin.rank,
                totalSupply = piratePlaceCoin.totalSupply,
                circulatingSupply = piratePlaceCoin.circulatingSupply,
                volume24h = null,
                dilutedMarketCap = piratePlaceCoin.fullyDilutedValuation["usd"],
                tvl = null,
            )
        )

    private fun mapPerformance(
        currency: String,
        changes: Changes
    ): Pair<String, Map<String, BigDecimal?>> =
        currency to mapOf(
            "7d" to changes.price.percentage7d[currency],
            "30d" to changes.price.percentage30d[currency]
        )

    private fun mapLinks(
        links: Links
    ): Map<String, String> =
        mapOf(
            "homepage" to links.homepage.firstOrNull().orEmpty(),
            "blockchain" to links.blockchainSite.firstOrNull().orEmpty(),
            "official forum" to links.officialForumUrl.orEmpty(),
            "chat" to links.chatUrl.orEmpty(),
            "announcement" to links.announcementUrl.orEmpty(),
            "facebook" to links.facebookUsername.orEmpty(),
            "reddit" to links.subredditUrl.orEmpty(),
            "twitter" to links.twitterScreenName.orEmpty(),
            "telegram" to links.telegramChannelIdentifier.orEmpty()
        )
}
