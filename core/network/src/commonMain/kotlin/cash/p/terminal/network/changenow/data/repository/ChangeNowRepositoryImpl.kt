package cash.p.terminal.network.changenow.data.repository

import cash.p.terminal.network.changenow.api.ChangeNowApi
import cash.p.terminal.network.changenow.data.entity.request.NewTransactionRequest
import cash.p.terminal.network.changenow.data.mapper.ChangeNowMapper
import cash.p.terminal.network.changenow.domain.repository.ChangeNowRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class ChangeNowRepositoryImpl(
    private val changeNowApi: ChangeNowApi,
    private val changeNowMapper: ChangeNowMapper
) : ChangeNowRepository {
    override suspend fun getAvailableCurrencies() = withContext(Dispatchers.IO) {
        changeNowApi.getAvailableCurrencies().let(changeNowMapper::mapCurrencyDtoToCurrency)
    }

    override suspend fun getExchangeAmount(
        tickerFrom: String,
        tickerTo: String,
        amount: BigDecimal
    ) = withContext(Dispatchers.IO) {
        changeNowApi.getExchangeAmount(tickerFrom, tickerTo, amount)
            .let(changeNowMapper::mapExchangeAmountDto)
    }

    override suspend fun getMinAmount(
        tickerFrom: String,
        tickerTo: String
    ) = withContext(Dispatchers.IO) {
        changeNowApi.getMinAmount(tickerFrom, tickerTo)
            .let(changeNowMapper::mapMinAmountDto)
    }

    override suspend fun createTransaction(
        newTransactionRequest: NewTransactionRequest
    ) = withContext(Dispatchers.IO) {
        changeNowApi.createTransaction(newTransactionRequest)
            .let(changeNowMapper::mapNewTransactionResponseDto)
    }

    override suspend fun getTransactionStatus(
        transactionId: String
    ) = withContext(Dispatchers.IO) {
        changeNowApi.getTransactionStatus(transactionId)
            .let(changeNowMapper::mapTransactionStatusDto)
    }
}