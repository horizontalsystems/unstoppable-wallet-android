package com.quantum.wallet.bankwallet.core.adapters

import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.core.ICoinManager
import com.quantum.wallet.bankwallet.core.managers.SolanaKitWrapper
import com.quantum.wallet.bankwallet.entities.TransactionValue
import com.quantum.wallet.bankwallet.entities.nft.NftUid
import com.quantum.wallet.bankwallet.entities.transactionrecords.evm.TransferEvent
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaTransactionRecord
import com.quantum.wallet.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import com.quantum.wallet.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.solanakit.models.FullTransaction
import java.math.BigDecimal

class SolanaTransactionConverter(
        private val coinManager: ICoinManager,
        private val source: TransactionSource,
        private val baseToken: Token,
        solanaKitWrapper: SolanaKitWrapper
) {
    private val userAddress = solanaKitWrapper.solanaKit.receiveAddress

    suspend fun transactionRecord(fullTransaction: FullTransaction): SolanaTransactionRecord {
        val transaction = fullTransaction.transaction
        val incomingTransfers = mutableListOf<SolanaTransactionRecord.Transfer>()
        val outgoingTransfers = mutableListOf<SolanaTransactionRecord.Transfer>()

        transaction.amount?.let {
            if (transaction.from == userAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, it.multiply(BigDecimal.valueOf(-1)).movePointLeft(baseToken.decimals))
                outgoingTransfers.add(SolanaTransactionRecord.Transfer(transaction.to, transactionValue))
            } else if (transaction.to == userAddress) {
                val transactionValue = TransactionValue.CoinValue(baseToken, it.movePointLeft(baseToken.decimals))
                incomingTransfers.add(SolanaTransactionRecord.Transfer(transaction.from, transactionValue))
            } else {}
        }

        for (fullTokenTransfer in fullTransaction.tokenTransfers) {
            val tokenTransfer = fullTokenTransfer.tokenTransfer
            val mintAccount = fullTokenTransfer.mintAccount
            val query = TokenQuery(BlockchainType.Solana, TokenType.Spl(tokenTransfer.mintAddress))
            val token = coinManager.getToken(query)

            val transactionValue = when {
                token != null -> TransactionValue.CoinValue(token, tokenTransfer.amount.movePointLeft(token.decimals))
                mintAccount.isNft -> TransactionValue.NftValue(
                    NftUid.Solana(mintAccount.address),
                    tokenTransfer.amount,
                    mintAccount.name,
                    mintAccount.symbol
                )
                else -> TransactionValue.RawValue(value = tokenTransfer.amount.toBigInteger())
            }

            if (tokenTransfer.incoming) {
                incomingTransfers.add(SolanaTransactionRecord.Transfer(transaction.from, transactionValue))
            } else {
                outgoingTransfers.add(SolanaTransactionRecord.Transfer(transaction.to, transactionValue))
            }
        }

        return when {
            (incomingTransfers.size == 1 && outgoingTransfers.isEmpty()) -> {
                val transfer = incomingTransfers.first()
                val spam = App.spamManager.isSpam(
                    transaction.hash.toByteArray(),
                    listOf(TransferEvent(transfer.address, transfer.value)),
                    source,
                    transaction.timestamp,
                    null
                )
                SolanaIncomingTransactionRecord(transaction, baseToken, source, transfer.address, transfer.value, spam)
            }

            (incomingTransfers.isEmpty() && outgoingTransfers.size == 1) -> {
                val transfer = outgoingTransfers.first()
                SolanaOutgoingTransactionRecord(transaction, baseToken, source, transfer.address, transfer.value, transfer.address == userAddress)
            }

            else -> {
                val incomingEvents = incomingTransfers.map { TransferEvent(it.address, it.value) }
                val spam = if (incomingEvents.isNotEmpty()) {
                    val outgoingEvents = outgoingTransfers.map { TransferEvent(it.address, it.value) }
                    App.spamManager.isSpam(
                        transaction.hash.toByteArray(),
                        incomingEvents + outgoingEvents,
                        source,
                        transaction.timestamp,
                        null
                    )
                } else {
                    false
                }
                SolanaUnknownTransactionRecord(transaction, baseToken, source, incomingTransfers, outgoingTransfers, spam)
            }
        }
    }

}
