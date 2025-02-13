package cash.p.terminal.network.pirate.data.mapper

import cash.p.terminal.network.data.requireNotNull
import cash.p.terminal.network.pirate.data.entity.CalculatorDataDto
import cash.p.terminal.network.pirate.data.entity.ChangeNowAssociatedCoinDto
import cash.p.terminal.network.pirate.data.entity.ChangesDto
import cash.p.terminal.network.pirate.data.entity.CommunityDataDto
import cash.p.terminal.network.pirate.data.entity.GraphUrlsDto
import cash.p.terminal.network.pirate.data.entity.InvestmentDataDto
import cash.p.terminal.network.pirate.data.entity.InvestmentGraphDataDto
import cash.p.terminal.network.pirate.data.entity.LinksDto
import cash.p.terminal.network.pirate.data.entity.MarketTickerDto
import cash.p.terminal.network.pirate.data.entity.PiratePlaceCoinDto
import cash.p.terminal.network.pirate.data.entity.StakeDataDto
import cash.p.terminal.network.pirate.domain.enity.CalculatorData
import cash.p.terminal.network.pirate.domain.enity.CalculatorItemData
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin
import cash.p.terminal.network.pirate.domain.enity.Changes
import cash.p.terminal.network.pirate.domain.enity.CoinPriceChart
import cash.p.terminal.network.pirate.domain.enity.CommunityData
import cash.p.terminal.network.pirate.domain.enity.GraphUrls
import cash.p.terminal.network.pirate.domain.enity.InvestmentData
import cash.p.terminal.network.pirate.domain.enity.InvestmentGraphData
import cash.p.terminal.network.pirate.domain.enity.Links
import cash.p.terminal.network.pirate.domain.enity.MarketCap
import cash.p.terminal.network.pirate.domain.enity.MarketTicker
import cash.p.terminal.network.pirate.domain.enity.PayoutType
import cash.p.terminal.network.pirate.domain.enity.PeriodType
import cash.p.terminal.network.pirate.domain.enity.PiratePlaceCoin
import cash.p.terminal.network.pirate.domain.enity.PriceChange
import cash.p.terminal.network.pirate.domain.enity.PricePoint
import cash.p.terminal.network.pirate.domain.enity.Stake
import cash.p.terminal.network.pirate.domain.enity.StakeData
import java.math.BigDecimal

internal class PiratePlaceMapper {
    fun mapInvestmentData(investmentDataDto: InvestmentDataDto) = InvestmentData(
        id = investmentDataDto.id,
        chain = investmentDataDto.chain,
        source = investmentDataDto.source,
        address = investmentDataDto.address,
        balance = investmentDataDto.balance,
        unrealizedValue = investmentDataDto.unrealizedValue,
        mint = investmentDataDto.mint,
        balancePrice = investmentDataDto.balancePrice,
        unrealizedValuePrice = investmentDataDto.unrealizedValuePrice,
        mintPrice = investmentDataDto.mintPrice
    )

    fun mapChangeNowCoinAssociationList(coinAssociations: List<ChangeNowAssociatedCoinDto>) =
        coinAssociations.map(::mapChangeNowCoinAssociation)

    private fun mapChangeNowCoinAssociation(coinAssociation: ChangeNowAssociatedCoinDto) =
        ChangeNowAssociatedCoin(
            ticker = coinAssociation.ticker.requireNotNull("ticker"),
            name = coinAssociation.name.requireNotNull("name"),
            blockchain = coinAssociation.blockchain.requireNotNull("blockchain"),
            coinId = coinAssociation.coinId.requireNotNull("coin_id")
        )

    fun mapInvestmentGraphData(investmentDataDto: InvestmentGraphDataDto) = InvestmentGraphData(
        points = investmentDataDto.points.map {
            PricePoint(
                value = it.value,
                balance = it.balance,
                from = it.from.toEpochMilli(),
                to = it.to.toEpochMilli(),
                price = it.price,
                balancePrice = it.balancePrice
            )
        }
    )

    fun mapStakeData(stakeDataDto: StakeDataDto) = StakeData(
        stakes = stakeDataDto.stakes.map {
            Stake(
                id = it.id,
                type = parsePayoutTypeFromServer(it.type),
                balance = it.balance,
                amount = it.amount,
                createdAt = it.createdAt.toEpochMilli(),
                balancePrice = it.balancePrice.orEmpty(),
                amountPrice = it.amountPrice.orEmpty()
            )
        }
    )

    fun mapCalculatorData(data: CalculatorDataDto) = CalculatorData(
        items = listOf(
            CalculatorItemData(
                periodType = PeriodType.DAY,
                amount = data.day.amount,
                price = data.day.price
            ),
            CalculatorItemData(
                periodType = PeriodType.WEEK,
                amount = data.week.amount,
                price = data.week.price
            ),
            CalculatorItemData(
                periodType = PeriodType.MONTH,
                amount = data.month.amount,
                price = data.month.price
            ),
            CalculatorItemData(
                periodType = PeriodType.YEAR,
                amount = data.year.amount,
                price = data.year.price
            )
        )
    )

    private fun parsePayoutTypeFromServer(value: String): PayoutType {
        return try {
            PayoutType.valueOf(value.uppercase())
        } catch (e: IllegalArgumentException) {
            // Handle unknown value, e.g., return a default
            PayoutType.UNKNOWN
        }
    }

    fun mapCoinInfo(coinInfo: PiratePlaceCoinDto) = PiratePlaceCoin(
        rank = coinInfo.rank,
        id = coinInfo.id,
        name = coinInfo.name,
        symbol = coinInfo.symbol,
        circulatingSupply = coinInfo.circulatingSupply,
        totalSupply = coinInfo.totalSupply,
        maxSupply = coinInfo.maxSupply,
        changes = mapChanges(coinInfo.changes),
        marketCap = coinInfo.marketCap,
        image = coinInfo.image,
        price = coinInfo.price,
        description = coinInfo.description,
        links = mapLinks(coinInfo.links),
        ath = coinInfo.ath,
        athPercentage = coinInfo.athPercentage,
        high24h = coinInfo.high24h,
        low24h = coinInfo.low24h,
        communityData = mapCommunityData(coinInfo.communityData),
        graphs = coinInfo.graphs.mapValues { mapGraphUrls(it.value) },
        isActive = coinInfo.isActive,
        isCurrency = coinInfo.isCurrency,
        isRealCurrency = coinInfo.isRealCurrency,
        updatedAt = coinInfo.updatedAt,
        fullyDilutedValuation = coinInfo.fullyDilutedValuation
    )

    private fun mapChanges(changes: ChangesDto) = Changes(
        price = PriceChange(
            percentage1h = changes.price.percentage1h,
            percentage24h = changes.price.percentage24h,
            percentage7d = changes.price.percentage7d,
            percentage30d = changes.price.percentage30d,
            percentage1y = changes.price.percentage1y
        ),
        marketCap = MarketCap(
            value24h = changes.marketCap.value24h
        )
    )

    private fun mapLinks(links: LinksDto) = Links(
        homepage = links.homepage,
        blockchainSite = links.blockchainSite,
        officialForumUrl = links.officialForumUrl,
        chatUrl = links.chatUrl,
        announcementUrl = links.announcementUrl,
        twitterScreenName = links.twitterScreenName,
        facebookUsername = links.facebookUsername,
        bitcointalkIdentifier = links.bitcointalkIdentifier,
        telegramChannelIdentifier = links.telegramChannelIdentifier,
        subredditUrl = links.subredditUrl
    )

    private fun mapCommunityData(communityData: CommunityDataDto) = CommunityData(
        facebookLikes = communityData.facebookLikes,
        twitterFollowers = communityData.twitterFollowers,
        redditSubscribers = communityData.redditSubscribers,
        redditAveragePosts48h = communityData.redditAveragePosts48h,
        redditAverageComments48h = communityData.redditAverageComments48h,
        redditAccountsActive48h = communityData.redditAccountsActive48h,
        telegramChannelUserCount = communityData.telegramChannelUserCount
    )

    private fun mapGraphUrls(mapGraphUrls: GraphUrlsDto) = GraphUrls(
        hour = mapGraphUrls.hour,
        day = mapGraphUrls.day,
        week = mapGraphUrls.week,
        month = mapGraphUrls.month,
        year = mapGraphUrls.year,
        max = mapGraphUrls.max
    )

    fun mapPricePoint(pricePoint: List<String>) = CoinPriceChart(
        timestamp = pricePoint.firstOrNull()?.toLongOrNull() ?: 0L,
        price = pricePoint.getOrNull(1)?.toBigDecimalOrNull() ?: BigDecimal.ZERO
    )

    fun mapMarketTicker(marketTicker: MarketTickerDto) = MarketTicker(
        fromSymbol = marketTicker.fromSymbol.orEmpty(),
        targetSymbol = marketTicker.targetSymbol.orEmpty(),
        targetId = marketTicker.targetId.orEmpty(),
        market = marketTicker.market.orEmpty(),
        marketUrl = marketTicker.marketUrl,
        tradeUrl = marketTicker.tradeUrl.orEmpty(),
        price = marketTicker.price ?: BigDecimal.ZERO,
        priceUsd = marketTicker.priceUsd ?: BigDecimal.ZERO,
        volume = marketTicker.volume ?: BigDecimal.ZERO,
        volumeUsd = marketTicker.volumeUsd ?: BigDecimal.ZERO,
        volumePercent = marketTicker.volumePercent ?: BigDecimal.ZERO,
        trustScore = marketTicker.trustScore.orEmpty()
    )
}