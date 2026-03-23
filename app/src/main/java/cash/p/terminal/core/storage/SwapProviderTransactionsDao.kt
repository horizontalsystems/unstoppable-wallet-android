package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.SwapProviderTransaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

@Dao
interface SwapProviderTransactionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(swapProviderTransaction: SwapProviderTransaction)

    @Query(
        "SELECT * FROM SwapProviderTransaction WHERE " +
                "((coinUidIn = :coinUid AND blockchainTypeIn = :blockchainType AND addressIn = :address) OR " +
                "(coinUidOut = :coinUid AND blockchainTypeOut = :blockchainType AND addressOut = :address)) AND " +
                "status not in (:statusesExcluded) ORDER BY date DESC LIMIT :limit"
    )
    fun getAll(
        coinUid: String,
        blockchainType: String,
        address: String,
        statusesExcluded: List<String>,
        limit: Int
    ): List<SwapProviderTransaction>

    @Query(
        "SELECT * FROM SwapProviderTransaction WHERE " +
                "((coinUidIn = :coinUid AND blockchainTypeIn = :blockchainType AND addressIn = :address) OR " +
                "(coinUidOut = :coinUid AND blockchainTypeOut = :blockchainType AND addressOut = :address)) " +
                "ORDER BY date DESC LIMIT :limit"
    )
    fun observeByToken(
        coinUid: String,
        blockchainType: String,
        address: String,
        limit: Int
    ): Flow<List<SwapProviderTransaction>>

    @Query("SELECT * FROM SwapProviderTransaction WHERE transactionId = :transactionId")
    suspend fun getTransaction(transactionId: String): SwapProviderTransaction?

    @Query("SELECT * FROM SwapProviderTransaction ORDER BY date DESC LIMIT 100")
    fun observeAll(): Flow<List<SwapProviderTransaction>>

    @Query(
        "SELECT * FROM SwapProviderTransaction WHERE " +
                "(coinUidIn = :coinUid AND blockchainTypeIn = :blockchainType AND date >= :dateFrom AND date <= :dateTo) " +
                "AND (:amountIn is NULL OR amountIn == :amountIn) ORDER BY date DESC LIMIT 1"
    )
    fun getByTokenIn(
        coinUid: String,
        amountIn: BigDecimal?,
        blockchainType: String,
        dateFrom: Long,
        dateTo: Long
    ): SwapProviderTransaction?

    @Query("SELECT * FROM SwapProviderTransaction WHERE outgoingRecordUid = :outgoingRecordUid")
    fun getByOutgoingRecordUid(
        outgoingRecordUid: String
    ): SwapProviderTransaction?

    @Query("SELECT * FROM SwapProviderTransaction WHERE incomingRecordUid = :incomingRecordUid")
    fun getByIncomingRecordUid(
        incomingRecordUid: String
    ): SwapProviderTransaction?

    @Query(
        "SELECT * FROM SwapProviderTransaction WHERE " +
                "(coinUidOut = :coinUid AND blockchainTypeOut = :blockchainType AND date >= :dateFrom AND date <= :dateTo) ORDER BY date DESC LIMIT 1"
    )
    fun getByTokenOut(
        coinUid: String,
        blockchainType: String,
        dateFrom: Long,
        dateTo: Long
    ): SwapProviderTransaction?

    @Query(
        """
        SELECT * FROM SwapProviderTransaction WHERE
        addressOut = :address
        AND blockchainTypeOut = :blockchainType
        AND coinUidOut = :coinUid
        AND incomingRecordUid IS NULL
        AND amountOutReal IS NOT NULL
        AND CAST(amountOutReal AS REAL) != 0
        AND ABS(CAST(amountOutReal AS REAL) - :amount) / CAST(amountOutReal AS REAL) < :tolerance
        AND (
            (finishedAt IS NOT NULL AND :timestamp >= finishedAt - :timeWindowMs AND :timestamp <= finishedAt + :timeWindowMs)
            OR
            (finishedAt IS NULL AND date >= :dateFrom AND date <= :dateTo)
        )
        ORDER BY ABS(CAST(amountOutReal AS REAL) - :amount) ASC, date ASC
        LIMIT 1
        """
    )
    fun getByAddressAndAmount(
        address: String,
        blockchainType: String,
        coinUid: String,
        amount: Double,
        tolerance: Double,
        timestamp: Long,
        timeWindowMs: Long,
        dateFrom: Long,
        dateTo: Long
    ): SwapProviderTransaction?

    @Query("UPDATE SwapProviderTransaction SET incomingRecordUid = :incomingRecordUid, amountOutReal = :amountOutReal WHERE date = :date")
    fun setIncomingRecordUid(date: Long, incomingRecordUid: String, amountOutReal: BigDecimal)

    @Query("UPDATE SwapProviderTransaction SET outgoingRecordUid = :outgoingRecordUid WHERE date = :date")
    fun setOutgoingRecordUid(date: Long, outgoingRecordUid: String)

    @Query("UPDATE SwapProviderTransaction SET status = :status, amountOutReal = :amountOutReal, finishedAt = :finishedAt WHERE transactionId = :transactionId")
    fun updateStatusFields(transactionId: String, status: String, amountOutReal: BigDecimal?, finishedAt: Long?)

    @Query(
        """
        SELECT * FROM SwapProviderTransaction
        WHERE coinUidOut = :coinUid
        AND blockchainTypeOut = :blockchainType
        AND incomingRecordUid IS NULL
        AND date >= :dateFrom
        AND date <= :dateTo
        AND CAST(amountOut AS REAL) != 0
        AND ABS(CAST(amountOut AS REAL) - :amount) / CAST(amountOut AS REAL) < :tolerance
        ORDER BY ABS(CAST(amountOut AS REAL) - :amount) ASC, date ASC
        LIMIT :limit
        """
    )
    fun getUnmatchedSwapsByTokenOut(
        coinUid: String,
        blockchainType: String,
        dateFrom: Long,
        dateTo: Long,
        amount: Double,
        tolerance: Double,
        limit: Int
    ): List<SwapProviderTransaction>

    @Query("""
        SELECT * FROM SwapProviderTransaction
        WHERE provider = :provider
        AND coinUidOut = :coinUidOut
        AND blockchainTypeOut = :blockchainTypeOut
        AND addressOut = :addressOut
        AND CAST(amountOut AS REAL) != 0
        AND ABS(CAST(amountOut AS REAL) - :expectedAmount) / CAST(amountOut AS REAL) < :tolerance
        AND date >= :dateFrom
        AND date <= :dateTo
        ORDER BY ABS(CAST(amountOut AS REAL) - :expectedAmount) ASC
        LIMIT 1
    """)
    fun getByProviderAndTokenOut(
        provider: String,
        coinUidOut: String,
        blockchainTypeOut: String,
        addressOut: String,
        expectedAmount: Double,
        tolerance: Double,
        dateFrom: Long,
        dateTo: Long,
    ): SwapProviderTransaction?
}
