package cash.p.terminal.network.exolix.domain.repository

import cash.p.terminal.network.exolix.data.entity.request.NewTransactionExolixRequest
import cash.p.terminal.network.exolix.domain.entity.ExolixNetwork
import cash.p.terminal.network.exolix.domain.entity.ExolixRate
import cash.p.terminal.network.exolix.domain.entity.ExolixTransaction
import java.math.BigDecimal

interface ExolixRepository {
    suspend fun getCurrencyNetworks(code: String): List<ExolixNetwork>

    suspend fun getRate(
        coinFrom: String,
        networkFrom: String,
        coinTo: String,
        networkTo: String,
        amount: BigDecimal,
        rateType: String,
    ): ExolixRate

    suspend fun createTransaction(
        newTransactionRequest: NewTransactionExolixRequest
    ): ExolixTransaction
}
