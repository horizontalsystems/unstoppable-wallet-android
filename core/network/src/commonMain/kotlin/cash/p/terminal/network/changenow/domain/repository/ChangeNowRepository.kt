package cash.p.terminal.network.changenow.domain.repository

import cash.p.terminal.network.changenow.data.entity.request.NewTransactionRequest
import cash.p.terminal.network.changenow.domain.entity.ChangeNowCurrency
import cash.p.terminal.network.changenow.domain.entity.ExchangeAmount
import cash.p.terminal.network.changenow.domain.entity.NewTransactionResponse
import java.math.BigDecimal

interface ChangeNowRepository {
    suspend fun getAvailableCurrencies(): List<ChangeNowCurrency>

    suspend fun getExchangeAmount(
        tickerFrom: String,
        tickerTo: String,
        amount: BigDecimal
    ): ExchangeAmount

    suspend fun getMinAmount(
        tickerFrom: String,
        tickerTo: String
    ): BigDecimal

    suspend fun createTransaction(
        newTransactionRequest: NewTransactionRequest
    ): NewTransactionResponse
}