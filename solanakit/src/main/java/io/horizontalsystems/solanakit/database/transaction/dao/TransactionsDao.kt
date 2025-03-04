package io.horizontalsystems.solanakit.database.transaction.dao

import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import io.horizontalsystems.solanakit.models.*
import io.horizontalsystems.solanakit.models.Transaction

@Dao
interface TransactionsDao {

    @Query("SELECT * FROM `Transaction` WHERE hash = :transactionHash LIMIT 1")
    fun get(transactionHash: String) : Transaction?

    @Query("SELECT * FROM `Transaction` WHERE NOT pending ORDER BY timestamp DESC LIMIT 1")
    fun lastNonPendingTransaction() : Transaction?

    @Query("SELECT * FROM `Transaction` WHERE pending ORDER BY timestamp")
    fun pendingTransactions() : List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTransactions(transactions: List<Transaction>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertTokenTransfers(tokenTransfers: List<TokenTransfer>)

    @Update
    fun updateTransactions(transactions: List<Transaction>)

    @RawQuery
    suspend fun getTransactions(query: SupportSQLiteQuery): List<FullTransactionWrapper>

    data class FullTransactionWrapper(
        @Embedded
        val transaction: Transaction,

        @Relation(
            entity = TokenTransfer::class,
            parentColumn = "hash",
            entityColumn = "transactionHash"
        )
        val tokenTransfersWithMintAccounts: List<TokenTransferAndMintAccount>
    ) {

        val fullTransaction: FullTransaction
            get() = FullTransaction(transaction, tokenTransfersWithMintAccounts.map { it.fullTokenTransfer })

    }

    data class TokenTransferAndMintAccount(
        @Embedded
        val tokenTransfer: TokenTransfer,

        @Relation(
            parentColumn = "mintAddress",
            entityColumn = "address"
        )
        val mintAccount: MintAccount
    ) {

        val fullTokenTransfer: FullTokenTransfer
            get() = FullTokenTransfer(tokenTransfer, mintAccount)

    }

}
