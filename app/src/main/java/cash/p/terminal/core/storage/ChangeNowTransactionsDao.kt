package cash.p.terminal.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cash.p.terminal.entities.ChangeNowTransaction
import java.math.BigDecimal

@Dao
interface ChangeNowTransactionsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(changeNowTransaction: ChangeNowTransaction)

    @Query(
        "SELECT * FROM ChangeNowTransaction WHERE " +
                "((coinUidIn = :coinUid AND blockchainTypeIn = :blockchainType AND addressIn = :address) OR " +
                "(coinUidOut = :coinUid AND blockchainTypeOut = :blockchainType AND addressOut = :address)) AND " +
                "status in (:statuses) ORDER BY date DESC LIMIT :limit"
    )
    fun getAll(
        coinUid: String,
        blockchainType: String,
        address: String,
        statuses: List<String>,
        limit: Int
    ): List<ChangeNowTransaction>

    @Query(
        "SELECT * FROM ChangeNowTransaction WHERE " +
                "(coinUidIn = :coinUid AND blockchainTypeIn = :blockchainType AND date >= :dateFrom AND date <= :dateTo) " +
                "AND (:amountIn is NULL OR amountIn == :amountIn) ORDER BY date DESC LIMIT 1"
    )
    fun getByTokenIn(
        coinUid: String,
        amountIn: BigDecimal?,
        blockchainType: String,
        dateFrom: Long,
        dateTo: Long
    ): ChangeNowTransaction?

    @Query(
        "SELECT * FROM ChangeNowTransaction WHERE " +
                "(coinUidOut = :coinUid AND blockchainTypeOut = :blockchainType AND date >= :dateFrom AND date <= :dateTo) ORDER BY date DESC LIMIT 1"
    )
    fun getByTokenOut(
        coinUid: String,
        blockchainType: String,
        dateFrom: Long,
        dateTo: Long
    ): ChangeNowTransaction?
}
