package cash.p.terminal.network.pirate.data.mapper

import cash.p.terminal.network.data.requireNotNull
import cash.p.terminal.network.pirate.data.entity.CalculatorDataDto
import cash.p.terminal.network.pirate.data.entity.ChangeNowAssociatedCoinDto
import cash.p.terminal.network.pirate.data.entity.InvestmentDataDto
import cash.p.terminal.network.pirate.data.entity.InvestmentGraphDataDto
import cash.p.terminal.network.pirate.data.entity.StakeDataDto
import cash.p.terminal.network.pirate.domain.enity.CalculatorData
import cash.p.terminal.network.pirate.domain.enity.CalculatorItemData
import cash.p.terminal.network.pirate.domain.enity.ChangeNowAssociatedCoin
import cash.p.terminal.network.pirate.domain.enity.InvestmentData
import cash.p.terminal.network.pirate.domain.enity.InvestmentGraphData
import cash.p.terminal.network.pirate.domain.enity.PayoutType
import cash.p.terminal.network.pirate.domain.enity.PeriodType
import cash.p.terminal.network.pirate.domain.enity.PricePoint
import cash.p.terminal.network.pirate.domain.enity.Stake
import cash.p.terminal.network.pirate.domain.enity.StakeData

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
}