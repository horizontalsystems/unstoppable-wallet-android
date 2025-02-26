package io.horizontalsystems.solanakit.transactions

import SplTokenAccountWithPublicKey
import android.util.Log
import com.solana.api.Api
import com.solana.api.SignatureInformation
import com.solana.core.PublicKey
import com.solana.programs.TokenProgram
import getTokenAccountsByOwner
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.database.transaction.TransactionStorage
import io.horizontalsystems.solanakit.models.FullTokenTransfer
import io.horizontalsystems.solanakit.models.FullTransaction
import io.horizontalsystems.solanakit.models.MintAccount
import io.horizontalsystems.solanakit.models.TokenTransfer
import io.horizontalsystems.solanakit.models.Transaction
import io.horizontalsystems.solanakit.noderpc.endpoints.getSignaturesForAddress
import java.math.BigDecimal

interface ITransactionListener {
    fun onUpdateTransactionSyncState(syncState: SolanaKit.SyncState)
}

class TransactionSyncer(
    private val publicKey: PublicKey,
    private val rpcClient: Api,
    private val storage: TransactionStorage,
    private val transactionManager: TransactionManager,
    private val pendingTransactionSyncer: PendingTransactionSyncer
) {
    var syncState: SolanaKit.SyncState =
        SolanaKit.SyncState.NotSynced(SolanaKit.SyncError.NotStarted())
        private set(value) {
            if (value != field) {
                field = value
                listener?.onUpdateTransactionSyncState(value)
            }
        }

    var listener: ITransactionListener? = null

    suspend fun sync() {
        if (syncState is SolanaKit.SyncState.Syncing) return

        syncState = SolanaKit.SyncState.Syncing()

        pendingTransactionSyncer.sync()

        val lastTransactionHash = storage.lastNonPendingTransaction()?.hash

        try {
            val rpcTransactions = getSignaturesFromRpcNode(
                pKey = publicKey,
                lastTransactionHash = lastTransactionHash
            ).apply { Log.d("TransactionSyncer", "rpcTransactions: ${this.size}") }
                .mapNotNull { it.signature }
                .mapNotNull { signature ->
                    getTransactionInfo(signature)
                }
            val splTransfers = getTokenAccountsByOwner().map {
                SplTokenAccountWithPublicKey(it.publicKey)
            }.map {
                getSignaturesFromRpcNode(
                    pKey = PublicKey.valueOf(it.publicKey),
                    lastTransactionHash = lastTransactionHash
                )
            }.flatten().apply { Log.d("TransactionSyncer", "token transactions: ${this.size}") }
                .mapNotNull { it.signature }
                .mapNotNull { signature ->
                    getTransactionInfo(signature)
                }
            val mintAddresses =
                splTransfers.mapNotNull { it.meta?.preTokenBalances?.firstOrNull()?.mint }.toSet()
                    .toList()
            val mintAccounts = getMintAccounts(mintAddresses)
            val transactions = merge(
                rpcTransactions = rpcTransactions + splTransfers,
                mintAccounts = mintAccounts
            )

            transactionManager.handle(transactions)
            syncState = SolanaKit.SyncState.Synced()
        } catch (exception: Throwable) {
            exception.printStackTrace()
            syncState = SolanaKit.SyncState.NotSynced(exception)
        }
    }

    private fun toBigNumWithMovePointLeft(value: Long?, shiftAmount: Int = 9) =
        value?.toBigDecimal()
            ?.movePointLeft(shiftAmount)?.stripTrailingZeros()

    private fun merge(
        rpcTransactions: List<TransactionResult>,
        mintAccounts: Map<String, MintAccount>
    ): List<FullTransaction> {
        val transactions = mutableMapOf<String, FullTransaction>()

        for (signatureInfo in rpcTransactions) {
            signatureInfo.blockTime.let { blockTime ->
                val postTokenBalances = signatureInfo.meta?.postTokenBalances?.firstOrNull()
                val preTokenBalances = signatureInfo.meta?.preTokenBalances?.firstOrNull()
                val amount = if (postTokenBalances != null && preTokenBalances != null) {
                    null //need to set amount NULL if TOKEN transfer
                } else {
                    BigDecimal(
                        (signatureInfo.meta?.preBalances?.getOrNull(0) ?: 0L) -
                                (signatureInfo.meta?.postBalances?.getOrNull(0) ?: 0L)
                    )
                }
                val transaction = Transaction(
                    hash = signatureInfo.transaction?.signatures?.firstOrNull().orEmpty(),
                    timestamp = blockTime,
                    fee = toBigNumWithMovePointLeft(signatureInfo.meta?.fee),
                    from = signatureInfo.transaction?.message?.accountKeys?.firstOrNull().orEmpty(),
                    to = signatureInfo.transaction?.message?.accountKeys?.getOrNull(1).orEmpty(),
                    error = signatureInfo.meta?.err?.toString(),
                    amount = amount,
                    pending = false
                )
                var tokenTransfers: List<FullTokenTransfer> = emptyList()
                if (postTokenBalances != null && preTokenBalances != null) {
                    val amount = (preTokenBalances.uiTokenAmount.amount?.toBigDecimal()
                        ?: BigDecimal.ZERO) - (postTokenBalances.uiTokenAmount.amount?.toBigDecimal()
                        ?: BigDecimal.ZERO)
                    mintAccounts[postTokenBalances.mint]?.let { mintAccount ->
                        tokenTransfers = listOf(
                            FullTokenTransfer(
                                tokenTransfer = TokenTransfer(
                                    transactionHash = signatureInfo.transaction?.signatures?.firstOrNull()
                                        .orEmpty(),
                                    mintAddress = postTokenBalances.mint,
                                    incoming = amount > BigDecimal.ZERO,
                                    amount = amount.abs()
                                ),
                                mintAccount = mintAccount
                            )
                        )
                    }
                }
                transactions[signatureInfo.transaction?.signatures?.firstOrNull().orEmpty()] =
                    FullTransaction(transaction = transaction, tokenTransfers = tokenTransfers)
            }
        }
        return transactions.values.toList()
    }

    private suspend fun getSignaturesFromRpcNode(
        pKey: PublicKey,
        lastTransactionHash: String?
    ): List<SignatureInformation> {
        val signatureObjects = mutableListOf<SignatureInformation>()
        var signatureObjectsChunk = listOf<SignatureInformation>()

        do {
            val lastSignature = signatureObjectsChunk.lastOrNull()?.signature
            signatureObjectsChunk = getSignaturesChunk(
                lastTransactionHash = lastTransactionHash,
                pKey = pKey,
                before = lastSignature
            )
            signatureObjects.addAll(signatureObjectsChunk)

        } while (signatureObjectsChunk.size == rpcSignaturesCount)

        return signatureObjects
    }

    private suspend fun getTokenAccountsByOwner(): List<SplTokenAccountWithPublicKey> {
        return rpcClient.getTokenAccountsByOwner(publicKey).getOrNull() ?: listOf()
    }

    private suspend fun getTransactionInfo(signature: String): TransactionResult? =
        rpcClient.getTransaction(signature).getOrNull()

    private suspend fun getSignaturesChunk(
        lastTransactionHash: String?,
        pKey: PublicKey,
        before: String? = null
    ): List<SignatureInformation> {
        return rpcClient.getSignaturesForAddress(
            account = pKey,
            until = lastTransactionHash,
            before = before,
            limit = rpcSignaturesCount
        ).getOrNull() ?: listOf()
    }

    private suspend fun getMintAccounts(mintAddresses: List<String>): Map<String, MintAccount> {
        if (mintAddresses.isEmpty()) {
            return mutableMapOf()
        }

        val publicKeys = mintAddresses.map { PublicKey.valueOf(it) }

        val mintAccounts = mutableMapOf<String, MintAccount>()

        try {
            rpcClient.getMultipleMintAccountsInfo(
                accounts = publicKeys
            ).getOrThrow()?.forEachIndexed { index, account ->
                val owner = account.owner
                val mint = account.data
                if (owner != tokenProgramId || mint == null) return@forEachIndexed
                val mintAddress = mintAddresses.getOrNull(index) ?: return@forEachIndexed

                val isNft = when {
                    mint.parsed.info.decimals != 0 -> false
                    mint.parsed.info.supply == "1" && mint.parsed.info.mintAuthority == null -> true
                    else -> false
                }
                mintAccounts[mintAddress] = MintAccount(
                    address = mintAddress,
                    decimals = mint.parsed.info.decimals,
                    supply = mint.parsed.info.supply.toLongOrNull(),
                    isNft = isNft,
                )
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return mintAccounts
    }

    companion object {
        val tokenProgramId = TokenProgram.PROGRAM_ID.toBase58()
        const val rpcSignaturesCount = 2
    }

}
