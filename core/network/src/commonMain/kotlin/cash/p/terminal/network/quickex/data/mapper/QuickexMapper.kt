package cash.p.terminal.network.quickex.data.mapper

import cash.p.terminal.network.quickex.data.entity.NewTransactionQuickexResponseDto
import cash.p.terminal.network.quickex.data.entity.QuickexInstrumentDto
import cash.p.terminal.network.quickex.data.entity.QuickexRatesDto
import cash.p.terminal.network.quickex.data.entity.TransactionQuickexStatusDto
import cash.p.terminal.network.quickex.domain.entity.ClaimedPublicRate
import cash.p.terminal.network.quickex.domain.entity.DepositAddress
import cash.p.terminal.network.quickex.domain.entity.Instrument
import cash.p.terminal.network.quickex.domain.entity.InstrumentInfo
import cash.p.terminal.network.quickex.domain.entity.NewTransactionQuickexResponse
import cash.p.terminal.network.quickex.domain.entity.OrderEvent
import cash.p.terminal.network.quickex.domain.entity.OrderEventKind
import cash.p.terminal.network.quickex.domain.entity.Pair
import cash.p.terminal.network.quickex.domain.entity.QuickexInstrument
import cash.p.terminal.network.quickex.domain.entity.QuickexRates
import cash.p.terminal.network.quickex.domain.entity.Rules
import cash.p.terminal.network.quickex.domain.entity.TransactionQuickexStatus
import timber.log.Timber

internal class QuickexMapper {
    fun mapCurrencyDtoToCurrency(list: List<QuickexInstrumentDto>) =
        list.map {
            QuickexInstrument(
                currencyTitle = it.currencyTitle,
                networkTitle = it.networkTitle,
                currencyFriendlyTitle = it.currencyFriendlyTitle,
                slug = it.slug,
                precisionDecimals = it.precisionDecimals,
                bestChangeName = it.bestChangeName,
                contractAddress = it.contractAddress
            )
        }

    fun mapRatesDto(dto: QuickexRatesDto) = QuickexRates(
        depositRules = dto.depositRules?.let {
            Rules(
                minAmount = it.minAmount.toBigDecimal(),
                maxAmount = it.maxAmount.toBigDecimal()
            )
        },
        minConfirmationsToWithdraw = dto.minConfirmationsToWithdraw,
        minConfirmationsToTrade = dto.minConfirmationsToTrade,
        instrumentFrom = Instrument(
            currencyTitle = dto.instrumentFrom.currencyTitle,
            networkTitle = dto.instrumentFrom.networkTitle,
            precisionDecimals = dto.instrumentFrom.precisionDecimals
        ),
        instrumentTo = Instrument(
            currencyTitle = dto.instrumentTo.currencyTitle,
            networkTitle = dto.instrumentTo.networkTitle,
            precisionDecimals = dto.instrumentTo.precisionDecimals
        ),
        amountToGet = dto.amountToGet.toBigDecimal(),
        price = dto.price.toBigDecimal()
    )

    fun mapNewTransactionQuickexResponseDto(dto: NewTransactionQuickexResponseDto) =
        NewTransactionQuickexResponse(
            depositAddress = DepositAddress(
                instrument = InstrumentInfo(
                    currencyTitle = dto.depositAddress.instrument.currencyTitle,
                    networkTitle = dto.depositAddress.instrument.networkTitle
                ),
                depositAddress = dto.depositAddress.depositAddress,
                depositAddressMemo = dto.depositAddress.depositAddressMemo,
            ),
            orderId = dto.orderId,
            pair = Pair(
                instrumentFrom = InstrumentInfo(
                    currencyTitle = dto.pair.instrumentFrom.currencyTitle,
                    networkTitle = dto.pair.instrumentFrom.networkTitle
                ),
                instrumentTo = InstrumentInfo(
                    currencyTitle = dto.pair.instrumentTo.currencyTitle,
                    networkTitle = dto.pair.instrumentTo.networkTitle
                )
            ),
            claimedDepositAmount = dto.claimedDepositAmount.toBigDecimal(),
            amountToGet = dto.amountToGet.toBigDecimal(),
            claimedPublicRate = ClaimedPublicRate(
                price = dto.claimedPublicRate.price.toBigDecimal(),
                claimedAmountToReceive = dto.claimedPublicRate.claimedAmountToReceive.toBigDecimal(),
                finalNetworkFeeAmount = dto.claimedPublicRate.finalNetworkFeeAmount?.toBigDecimal()
            ),
        )

    fun mapTransactionQuickexStatusDto(dto: TransactionQuickexStatusDto) = TransactionQuickexStatus(
        orderId = dto.orderId,
        createdAt = dto.createdAt,
        orderEvents = dto.orderEvents.map { eventDto ->
            OrderEvent(
                kind = mapOrderEventKind(eventDto.kind),
                createdAt = eventDto.createdAt
            )
        },
        completed = dto.completed,
        withdrawalAmount = dto.withdrawals?.firstOrNull()?.amount?.toBigDecimalOrNull()
    )

    private fun mapOrderEventKind(kind: String): OrderEventKind? {
        return try {
            OrderEventKind.valueOf(kind)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Unknown OrderEventKind: $kind")
            null
        }
    }
}