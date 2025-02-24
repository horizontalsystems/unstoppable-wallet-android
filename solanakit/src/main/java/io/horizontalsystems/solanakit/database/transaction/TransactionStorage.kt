package io.horizontalsystems.solanakit.database.transaction

import androidx.sqlite.db.SimpleSQLiteQuery
import io.horizontalsystems.solanakit.models.FullTokenAccount
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.LastSyncedTransaction
import io.horizontalsystems.solanakit.models.MintAccount
import io.horizontalsystems.solanakit.models.TokenAccount
import io.horizontalsystems.solanakit.models.Transaction

class TransactionStorage(
    database: TransactionDatabase,
    private val address: String
) {
    private val syncerStateDao = database.transactionSyncerStateDao()
    private val transactionsDao = database.transactionsDao()
    private val mintAccountDao = database.mintAccountDao()
    private val tokenAccountDao = database.tokenAccountsDao()

    fun getSyncedBlockTime(syncerId: String): LastSyncedTransaction? =
        syncerStateDao.get(syncerId)

    fun setSyncedBlockTime(syncBlockTime: LastSyncedTransaction) {
        syncerStateDao.save(syncBlockTime)
    }

    fun lastNonPendingTransaction(): Transaction? =
        transactionsDao.lastNonPendingTransaction()

    fun pendingTransactions(): List<Transaction> =
        transactionsDao.pendingTransactions()

    fun updateTransactions(transactions: List<Transaction>) =
        transactionsDao.updateTransactions(transactions)

    fun addTransactions(transactions: List<FullTransaction>) {
        transactionsDao.insertTransactions(transactions.map { it.transaction })

        val fullTokenTransfers = transactions.map { it.tokenTransfers }.flatten()
        transactionsDao.insertTokenTransfers(fullTokenTransfers.map { it.tokenTransfer })
        mintAccountDao.insert(fullTokenTransfers.map { it.mintAccount }.toSet().toList())
    }

    suspend fun getTransactions(
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> {
        val condition = incoming?.let {
            if (incoming) "((tx.amount IS NOT NULL AND tx.`to` = '$address') OR tt.incoming)"
            else "((tx.amount IS NOT NULL AND tx.`from` = '$address') OR NOT(tt.incoming))"
        }

        return getTransactions(condition, incoming != null, fromHash, limit)
    }

    suspend fun getSolTransactions(
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> {
        val condition = incoming?.let {
            if (incoming) "(tx.amount IS NOT NULL AND tx.`to` = '$address')"
            else "(tx.amount IS NOT NULL AND tx.`from` = '$address')"
        } ?: "tx.amount IS NOT NULL"

        return getTransactions(condition, false, fromHash, limit)
    }

    suspend fun getSplTransactions(
        mintAddress: String,
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> {
        val condition = incoming?.let {
            val incomingCondition = if (incoming) "tt.incoming" else "NOT(tt.incoming)"
            "(tt.mintAddress = '$mintAddress' AND $incomingCondition)"
        } ?: "tt.mintAddress = '$mintAddress'"

        return getTransactions(condition, true, fromHash, limit)
    }

    private suspend fun getTransactions(
        typeCondition: String?,
        joinTokenTransfers: Boolean,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> {
        val whereConditions = mutableListOf<String>()
        typeCondition?.let { whereConditions.add(it) }

        fromHash?.let { transactionsDao.get(it) }?.let { fromTransaction ->
            val fromCondition = """
                           (
                                tx.timestamp < ${fromTransaction.timestamp} OR
                                (
                                    tx.timestamp = ${fromTransaction.timestamp} AND
                                    HEX(tx.hash) < "${fromTransaction.hash}"
                                )
                           )
                           """

            whereConditions.add(fromCondition)
        }

        val whereClause =
            if (whereConditions.isNotEmpty()) "WHERE ${whereConditions.joinToString(" AND ")}" else ""
        val orderClause = "ORDER BY tx.timestamp DESC, HEX(tx.hash) DESC"
        val limitClause = limit?.let { "LIMIT $limit" } ?: ""

        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` AS tx
                      ${if (joinTokenTransfers) "LEFT JOIN TokenTransfer AS tt ON tx.hash = tt.transactionHash" else ""}
                      $whereClause
                      $orderClause
                      $limitClause
                      """

        return transactionsDao.getTransactions(SimpleSQLiteQuery(sqlQuery))
            .map { it.fullTransaction }
    }

    suspend fun getMintAccount(address: String): MintAccount? =
        mintAccountDao.get(address)

    suspend fun getFullTransactions(hashes: List<String>): List<FullTransaction> {
        val sqlQuery = """
                      SELECT tx.*
                      FROM `Transaction` AS tx
                      LEFT JOIN TokenTransfer AS tt ON tx.hash = tt.transactionHash
                      WHERE tx.hash IN (${hashes.joinToString(", ", "'", "'")})
                      """

        return transactionsDao.getTransactions(SimpleSQLiteQuery(sqlQuery))
            .map { it.fullTransaction }
    }

    fun saveTokenAccounts(tokenAccounts: List<TokenAccount>) {
        tokenAccountDao.insert(tokenAccounts)
    }

    fun saveMintAccounts(mintAccounts: List<MintAccount>) {
        mintAccountDao.insert(mintAccounts)
    }

    fun getTokenAccounts(mintAddresses: List<String>? = null): List<TokenAccount> =
        if (mintAddresses == null) tokenAccountDao.getAll()
        else tokenAccountDao.get(mintAddresses)

    fun getFullTokenAccount(mintAddress: String): FullTokenAccount? =
        tokenAccountDao.get(mintAddress)?.fullTokenAccount

    fun getFullTokenAccounts(): List<FullTokenAccount> =
        tokenAccountDao.getAllFullAccounts().map { it.fullTokenAccount }

    fun tokenAccountExists(mintAddress: String): Boolean =
        tokenAccountDao.getByMintAddress(mintAddress) != null

    fun addTokenAccount(tokenAccount: TokenAccount) {
        tokenAccountDao.insert(tokenAccount)
    }

    fun addMintAccount(mintAccount: MintAccount) {
        mintAccountDao.insert(mintAccount)
    }
}
