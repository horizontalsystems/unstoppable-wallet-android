package cash.p.terminal.core.storage

import cash.p.terminal.core.utils.SwapTransactionMatcher
import cash.p.terminal.entities.SwapProviderTransaction
import cash.p.terminal.network.swaprepository.SwapProvider
import cash.p.terminal.wallet.Token
import io.horizontalsystems.core.DispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class SwapProviderTransactionsStorage(
    private val dao: SwapProviderTransactionsDao,
    private val dispatcherProvider: DispatcherProvider
) {

    private companion object Companion {
        const val THRESHOLD_MSEC = 40_000
        const val AMOUNT_TOLERANCE = 0.005 // 0.5%
        const val FINISHED_AT_WINDOW_MS = 1_800_000L // ±30 minutes
        const val LEGACY_WINDOW_BEFORE_MS = 3_600_000L  // 1 hour before
        const val LEGACY_WINDOW_AFTER_MS = 10_800_000L  // 3 hours after
    }

    fun save(
        swapProviderTransaction: SwapProviderTransaction
    ) = dao.insert(swapProviderTransaction)

    fun getAll(
        token: Token,
        address: String,
        statusesExcluded: List<String>,
        limit: Int
    ) = dao.getAll(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        address = address,
        statusesExcluded = statusesExcluded,
        limit = limit
    )

    fun observeByToken(
        token: Token,
        address: String,
        limit: Int = 50
    ): Flow<List<SwapProviderTransaction>> = dao.observeByToken(
        coinUid = token.coin.uid,
        blockchainType = token.blockchainType.uid,
        address = address,
        limit = limit
    )

    fun observeAll(): Flow<List<SwapProviderTransaction>> = dao.observeAll()

    suspend fun getTransaction(transactionId: String) =
        withContext(dispatcherProvider.io) {
            dao.getTransaction(transactionId)
        }

    fun getByCoinUidIn(
        coinUid: String,
        blockchainType: String,
        amountIn: BigDecimal?,
        timestamp: Long
    ) = dao.getByTokenIn(
        coinUid = coinUid,
        amountIn = amountIn,
        blockchainType = blockchainType,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )

    fun getByOutgoingRecordUid(
        outgoingRecordUid: String
    ) = dao.getByOutgoingRecordUid(
        outgoingRecordUid = outgoingRecordUid
    )

    fun getByIncomingRecordUid(
        incomingRecordUid: String
    ) = dao.getByIncomingRecordUid(
        incomingRecordUid = incomingRecordUid
    )

    fun getByTokenOut(
        coinUid: String,
        blockchainType: String,
        timestamp: Long
    ) = dao.getByTokenOut(
        coinUid = coinUid,
        blockchainType = blockchainType,
        dateFrom = timestamp - THRESHOLD_MSEC,
        dateTo = timestamp + THRESHOLD_MSEC
    )

    fun getByAddressAndAmount(
        address: String,
        blockchainType: String,
        coinUid: String,
        amount: BigDecimal,
        timestamp: Long
    ): SwapProviderTransaction? = dao.getByAddressAndAmount(
        address = address,
        blockchainType = blockchainType,
        coinUid = coinUid,
        amount = amount.toDouble(),
        tolerance = AMOUNT_TOLERANCE,
        timestamp = timestamp,
        timeWindowMs = FINISHED_AT_WINDOW_MS,
        dateFrom = timestamp - SwapTransactionMatcher.TIME_WINDOW_MS,
        dateTo = timestamp + SwapTransactionMatcher.TIME_WINDOW_MS
    )

    fun setIncomingRecordUid(date: Long, incomingRecordUid: String, amountOutReal: BigDecimal) =
        dao.setIncomingRecordUid(date, incomingRecordUid, amountOutReal)

    fun setOutgoingRecordUid(date: Long, outgoingRecordUid: String) =
        dao.setOutgoingRecordUid(date, outgoingRecordUid)

    fun updateStatusFields(
        transactionId: String,
        status: String,
        amountOutReal: BigDecimal?,
        finishedAt: Long?
    ) = dao.updateStatusFields(transactionId, status, amountOutReal, finishedAt)

    fun getByProviderAndTokenOut(
        provider: SwapProvider,
        coinUidOut: String,
        blockchainTypeOut: String,
        addressOut: String,
        expectedAmount: BigDecimal,
        legStartTime: Long,
    ): SwapProviderTransaction? = dao.getByProviderAndTokenOut(
        provider = provider.name,
        coinUidOut = coinUidOut,
        blockchainTypeOut = blockchainTypeOut,
        addressOut = addressOut,
        expectedAmount = expectedAmount.toDouble(),
        tolerance = AMOUNT_TOLERANCE,
        dateFrom = legStartTime - LEGACY_WINDOW_BEFORE_MS,
        dateTo = legStartTime + LEGACY_WINDOW_AFTER_MS,
    )

    fun getUnmatchedSwapsByTokenOut(
        coinUid: String,
        blockchainType: String,
        fromTimestamp: Long,
        toTimestamp: Long,
        amount: BigDecimal,
        tolerance: Double,
        limit: Int = 100
    ): List<SwapProviderTransaction> = dao.getUnmatchedSwapsByTokenOut(
        coinUid = coinUid,
        blockchainType = blockchainType,
        dateFrom = fromTimestamp,
        dateTo = toTimestamp,
        amount = amount.toDouble(),
        tolerance = tolerance,
        limit = limit
    )
}

fun List<SwapProviderTransaction>.toRecordUidMap(): Map<String, SwapProviderTransaction> {
    return flatMap { swap ->
        listOfNotNull(
            swap.incomingRecordUid?.let { it to swap },
            swap.outgoingRecordUid?.let { it to swap }
        )
    }.toMap()
}
