package cash.p.terminal.network.exolix.data.repository

import cash.p.terminal.network.changenow.domain.entity.TransactionStatusEnum
import cash.p.terminal.network.exolix.api.ExolixApi
import cash.p.terminal.network.exolix.data.entity.request.NewTransactionExolixRequest
import cash.p.terminal.network.exolix.data.mapper.ExolixMapper
import cash.p.terminal.network.exolix.domain.repository.ExolixRepository
import cash.p.terminal.network.swaprepository.SwapProviderTransactionStatusRepository
import cash.p.terminal.network.swaprepository.SwapProviderTransactionStatusResult
import cash.p.terminal.network.swaprepository.parseIsoTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigDecimal

internal class ExolixRepositoryImpl(
    private val exolixApi: ExolixApi,
    private val exolixMapper: ExolixMapper,
) : ExolixRepository, SwapProviderTransactionStatusRepository {
    override suspend fun getCurrencyNetworks(code: String) = withContext(Dispatchers.IO) {
        exolixApi.getCurrencyNetworks(code)
            .let(exolixMapper::mapNetworksDto)
    }

    override suspend fun getRate(
        coinFrom: String,
        networkFrom: String,
        coinTo: String,
        networkTo: String,
        amount: BigDecimal,
        rateType: String,
    ) = withContext(Dispatchers.IO) {
        exolixApi.getRate(
            coinFrom = coinFrom,
            networkFrom = networkFrom,
            coinTo = coinTo,
            networkTo = networkTo,
            amount = amount,
            rateType = rateType,
        ).let(exolixMapper::mapRateDto)
    }

    override suspend fun createTransaction(
        newTransactionRequest: NewTransactionExolixRequest
    ) = withContext(Dispatchers.IO) {
        exolixApi.createTransaction(newTransactionRequest)
            .let(exolixMapper::mapTransactionDto)
    }

    override suspend fun getTransactionStatus(
        transactionId: String,
        destinationAddress: String,
    ): SwapProviderTransactionStatusResult = withContext(Dispatchers.IO) {
        val transaction = exolixApi.getTransaction(transactionId)
            .let(exolixMapper::mapTransactionDto)
        val status = transaction.status.toTransactionStatus()
        val finishedAt = if (status == TransactionStatusEnum.FINISHED) {
            transaction.updatedAt?.parseIsoTimestamp()
        } else {
            null
        }

        SwapProviderTransactionStatusResult(
            status = status,
            amountOutReal = transaction.amountTo,
            finishedAt = finishedAt,
        )
    }

    private fun String.toTransactionStatus(): TransactionStatusEnum = when (lowercase()) {
        "wait" -> TransactionStatusEnum.WAITING
        "confirmation", "confirmed" -> TransactionStatusEnum.CONFIRMING
        "exchanging" -> TransactionStatusEnum.EXCHANGING
        "sending" -> TransactionStatusEnum.SENDING
        "success" -> TransactionStatusEnum.FINISHED
        "overdue" -> TransactionStatusEnum.FAILED
        "refund" -> TransactionStatusEnum.WAITING
        "refunded" -> TransactionStatusEnum.REFUNDED
        else -> TransactionStatusEnum.UNKNOWN
    }

}
