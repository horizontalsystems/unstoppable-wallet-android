package io.horizontalsystems.solanakit.transactions

import com.solana.actions.Action
import com.solana.core.Account
import com.solana.core.PublicKey
import com.solana.core.TransactionInstruction
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.core.TokenAccountManager
import io.horizontalsystems.solanakit.database.transaction.TransactionStorage
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.models.FullTokenTransfer
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.TokenTransfer
import io.horizontalsystems.solanakit.models.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.await
import org.sol4k.Connection
import org.sol4k.RpcUrl
import org.sol4k.api.Commitment
import java.math.BigDecimal
import java.time.Instant

class TransactionManager(
    address: Address,
    private val storage: TransactionStorage,
    private val rpcAction: Action,
    private val tokenAccountManager: TokenAccountManager
) {

    private val addressString = address.publicKey.toBase58()
    private val _transactionsFlow = MutableStateFlow<List<FullTransaction>>(listOf())
    val transactionsFlow: StateFlow<List<FullTransaction>> = _transactionsFlow

    fun allTransactionsFlow(incoming: Boolean?): Flow<List<FullTransaction>> =
        _transactionsFlow.map { txList ->
            val incoming = incoming ?: return@map txList

            txList.filter { fullTransaction ->
                hasSolTransfer(
                    fullTransaction,
                    incoming
                ) || fullTransaction.tokenTransfers.any { it.tokenTransfer.incoming == incoming }
            }
        }.filter { it.isNotEmpty() }

    fun solTransactionsFlow(incoming: Boolean?): Flow<List<FullTransaction>> =
        _transactionsFlow.map { txList ->
            txList.filter { hasSolTransfer(it, incoming) }
        }.filter { it.isNotEmpty() }

    fun splTransactionsFlow(mintAddress: String, incoming: Boolean?): Flow<List<FullTransaction>> =
        _transactionsFlow.map { txList ->
            txList.filter { fullTransaction ->
                hasSplTransfer(mintAddress, fullTransaction.tokenTransfers, incoming)
            }
        }.filter { it.isNotEmpty() }


    suspend fun getAllTransaction(
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> =
        storage.getTransactions(incoming, fromHash, limit)

    suspend fun getSolTransaction(
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> =
        storage.getSolTransactions(incoming, fromHash, limit)

    suspend fun getSplTransaction(
        mintAddress: String,
        incoming: Boolean?,
        fromHash: String?,
        limit: Int?
    ): List<FullTransaction> =
        storage.getSplTransactions(mintAddress, incoming, fromHash, limit)

    suspend fun handle(syncedTransactions: List<FullTransaction>) {
        val existingMintAddresses = mutableListOf<String>()

        if (syncedTransactions.isNotEmpty()) {
            val existingTransactionsMap =
                storage.getFullTransactions(syncedTransactions.map { it.transaction.hash })
                    .groupBy { it.transaction.hash }
            val transactions = syncedTransactions.map { syncedTx ->
                val existingTx = existingTransactionsMap[syncedTx.transaction.hash]?.firstOrNull()

                if (existingTx == null) syncedTx
                else {
                    val syncedTxHeader = syncedTx.transaction
                    val existingTxHeader = existingTx.transaction

                    FullTransaction(
                        transaction = Transaction(
                            hash = syncedTxHeader.hash,
                            timestamp = syncedTxHeader.timestamp,
                            fee = syncedTxHeader.fee,
                            from = syncedTxHeader.from ?: existingTxHeader.from,
                            to = syncedTxHeader.to ?: existingTxHeader.to,
                            amount = syncedTxHeader.amount ?: existingTxHeader.amount,
                            error = syncedTxHeader.error,
                            pending = syncedTxHeader.pending,
                        ),
                        tokenTransfers = syncedTx.tokenTransfers.ifEmpty {
                            for (tokenTransfer in existingTx.tokenTransfers) {
                                existingMintAddresses.add(tokenTransfer.mintAccount.address)
                            }

                            existingTx.tokenTransfers
                        }
                    )
                }
            }

            storage.addTransactions(transactions)
            _transactionsFlow.tryEmit(transactions)
        }
    }

    fun notifyTransactionsUpdate(transactions: List<FullTransaction>) {
        _transactionsFlow.tryEmit(transactions)
    }

    private fun hasSolTransfer(fullTransaction: FullTransaction, incoming: Boolean?): Boolean {
        val amount = fullTransaction.transaction.amount ?: return false
        val incoming = incoming ?: return true

        return amount > BigDecimal.ZERO &&
                ((incoming && fullTransaction.transaction.to == addressString) || (!incoming && fullTransaction.transaction.from == addressString))
    }

    private fun hasSplTransfer(
        mintAddress: String,
        tokenTransfers: List<FullTokenTransfer>,
        incoming: Boolean?
    ): Boolean =
        tokenTransfers.any { fullTokenTransfer ->
            if (fullTokenTransfer.mintAccount.address != mintAddress) return false
            val incoming = incoming ?: return@any true

            fullTokenTransfer.tokenTransfer.incoming == incoming
        }

    suspend fun sendSol(toAddress: Address, amount: Long, signerAccount: Account): FullTransaction {
        val connection = Connection(RpcUrl.MAINNNET)
        val blockHash = connection.getLatestBlockhashExtended(Commitment.FINALIZED)
        val (transactionHash, base64Encoded) = rpcAction.sendSOL(
            account = signerAccount,
            destination = toAddress.publicKey,
            amount = amount,
            instructions = priorityFeeInstructions(),
            recentBlockHash = blockHash.blockhash
        ).await()

        val fullTransaction = FullTransaction(
            Transaction(
                hash = transactionHash,
                timestamp = Instant.now().epochSecond,
                fee = SolanaKit.fee,
                from = addressString,
                to = toAddress.publicKey.toBase58(),
                amount = amount.toBigDecimal(),
                pending = true,
                blockHash = blockHash.blockhash,
                lastValidBlockHeight = blockHash.lastValidBlockHeight,
                base64Encoded = base64Encoded
            ),
            listOf()
        )

        storage.addTransactions(listOf(fullTransaction))
        _transactionsFlow.tryEmit(listOf(fullTransaction))

        return fullTransaction
    }

    private fun priorityFeeInstructions(): List<TransactionInstruction> {
        val computeUnitLimit = ComputeBudgetProgram.setComputeUnitLimit(units = 300_000)
        val computeUnitPrice = ComputeBudgetProgram.setComputeUnitPrice(microLamports = 500_000)
        return listOf(computeUnitLimit, computeUnitPrice)
    }

    suspend fun sendSpl(
        mintAddress: Address,
        toAddress: Address,
        amount: Long,
        signerAccount: Account
    ): FullTransaction {
        val mintAddressString = mintAddress.publicKey.toBase58()
        val fullTokenAccount =
            tokenAccountManager.getFullTokenAccountByMintAddress(mintAddressString)
                ?: throw Exception("TokenAccount not found for $mintAddressString")
        val tokenAccount = fullTokenAccount.tokenAccount
        val mintAccount = fullTokenAccount.mintAccount

        val connection = Connection(RpcUrl.MAINNNET)
        val blockHash = connection.getLatestBlockhashExtended(Commitment.FINALIZED)

        val (transactionHash, base64Trx) = rpcAction.sendSPLTokens(
            mintAddress = mintAddress.publicKey,
            fromPublicKey = PublicKey(tokenAccount.address),
            destinationAddress = toAddress.publicKey,
            amount = amount,
            account = signerAccount,
            allowUnfundedRecipient = true,
            instructions = priorityFeeInstructions(),
            recentBlockHash = blockHash.blockhash
        ).await()

        val fullTransaction = FullTransaction(
            Transaction(
                hash = transactionHash,
                timestamp = Instant.now().epochSecond,
                from = addressString,
                to = toAddress.publicKey.toBase58(),
                fee = SolanaKit.fee,
                pending = true,
                blockHash = blockHash.blockhash,
                lastValidBlockHeight = blockHash.lastValidBlockHeight,
                base64Encoded = base64Trx
            ),
            listOf(
                FullTokenTransfer(
                    TokenTransfer(
                        transactionHash = transactionHash,
                        mintAddress = mintAddressString,
                        incoming = false,
                        amount = -amount.toBigDecimal()
                    ),
                    mintAccount
                )
            )
        )

        storage.addTransactions(listOf(fullTransaction))
        _transactionsFlow.tryEmit(listOf(fullTransaction))

        return fullTransaction
    }

}
