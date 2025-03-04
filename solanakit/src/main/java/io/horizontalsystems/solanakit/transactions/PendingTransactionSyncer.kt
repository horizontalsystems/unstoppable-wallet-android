package io.horizontalsystems.solanakit.transactions

import com.solana.api.Api
import com.solana.rxsolana.api.getBlockHeight
import io.horizontalsystems.solanakit.database.transaction.TransactionStorage
import io.horizontalsystems.solanakit.models.Transaction
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withTimeout
import org.sol4k.RpcUrl
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.logging.Logger

class PendingTransactionSyncer(
    private val rpcClient: Api,
    private val storage: TransactionStorage,
    private val transactionManager: TransactionManager
) {
    private val logger = Logger.getLogger("PendingTransactionSyncer")

    suspend fun sync() {
        val updatedTransactions = mutableListOf<Transaction>()

        val pendingTransactions = storage.pendingTransactions()
        val currentBlockHeight = try {
            rpcClient.getBlockHeight().await()
        } catch (error: Throwable) {
            return
        }

        pendingTransactions.forEach { pendingTx ->
            try {
                val confirmedTransaction = withTimeout(20000) {
                    rpcClient.getTransaction(pendingTx.hash)
                }

                confirmedTransaction.onSuccess { transaction ->
                    updatedTransactions.add(
                        pendingTx.copy(pending = false, error = transaction.meta?.err?.toString())
                    )
                }

            } catch (error: Throwable) {
                if (currentBlockHeight <= pendingTx.lastValidBlockHeight) {
                    sendTransaction(pendingTx.base64Encoded)

                    updatedTransactions.add(
                        pendingTx.copy(retryCount = pendingTx.retryCount + 1)
                    )
                } else {
                    updatedTransactions.add(
                        pendingTx.copy(pending = false, error = "BlockHash expired")
                    )
                }

                logger.info("getConfirmedTx exception ${error.message ?: error.javaClass.simpleName}")
            }
        }

        storage.updateTransactions(updatedTransactions)
        transactionManager.notifyTransactionsUpdate(storage.getFullTransactions(updatedTransactions.map { it.hash }))
    }

    private fun sendTransaction(encodedTransaction: String) {
        try {
            val connection = URL(RpcUrl.MAINNNET.value).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            connection.outputStream.use {

                val body = "{" +
                        "\"method\": \"sendTransaction\", " +
                        "\"jsonrpc\": \"2.0\", " +
                        "\"id\": ${System.currentTimeMillis()}, " +
                        "\"params\": [" +
                        "\"$encodedTransaction\", " +
                        "{" +
                        "\"encoding\": \"base64\"," +
                        "\"skipPreflight\": false," +
                        "\"preflightCommitment\": \"confirmed\"," +
                        "\"maxRetries\": 0" +
                        "}" +
                        "]" +
                        "}"

                it.write(body.toByteArray())
            }
            val responseBody = connection.inputStream.use {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    reader.readText()
                }
            }
            connection.disconnect()
        } catch (e: Throwable) {
        }
    }

}
