package cash.p.terminal.network.changenow.data.mapper

import cash.p.terminal.network.changenow.data.entity.ChangeNowCurrencyDto
import cash.p.terminal.network.changenow.data.entity.ExchangeAmountDto
import cash.p.terminal.network.changenow.data.entity.MinAmountDto
import cash.p.terminal.network.changenow.data.entity.NewTransactionResponseDto
import cash.p.terminal.network.changenow.domain.entity.ChangeNowCurrency
import cash.p.terminal.network.changenow.domain.entity.ExchangeAmount
import cash.p.terminal.network.changenow.domain.entity.NewTransactionResponse

internal class ChangeNowMapper {
    fun mapCurrencyDtoToCurrency(list: List<ChangeNowCurrencyDto>) =
        list.map {
            ChangeNowCurrency(
                ticker = it.ticker,
                name = it.name,
                image = it.image,
                hasExternalId = it.hasExternalId,
                isExtraIdSupported = it.isExtraIdSupported,
                isFiat = it.isFiat,
                featured = it.featured,
                isStable = it.isStable,
                supportsFixedRate = it.supportsFixedRate
            )
        }

    fun mapExchangeAmountDto(dto: ExchangeAmountDto) = ExchangeAmount(
        estimatedAmount = dto.estimatedAmount?.toBigDecimalOrNull(),
        transactionSpeedForecast = dto.transactionSpeedForecast,
        warningMessage = dto.warningMessage
    )

    fun mapMinAmountDto(dto: MinAmountDto) = dto.minAmount!!.toBigDecimal()

    fun mapNewTransactionResponseDto(dto: NewTransactionResponseDto) = NewTransactionResponse(
        id = dto.id,
        payinAddress = dto.payinAddress,
        payoutAddress = dto.payoutAddress,
        payoutExtraId = dto.payoutExtraId,
        fromCurrency = dto.fromCurrency,
        toCurrency = dto.toCurrency,
        refundAddress = dto.refundAddress,
        refundExtraId = dto.refundExtraId,
        amount = dto.amount.toBigDecimal()
    )
}