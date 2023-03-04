package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.SolanaKitWrapper
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
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

    fun transactionRecord(fullTransaction: FullTransaction): SolanaTransactionRecord {
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
                incomingTransfers.add(SolanaTransactionRecord.Transfer(null, transactionValue))
            } else {
                outgoingTransfers.add(SolanaTransactionRecord.Transfer(null, transactionValue))
            }
        }

        return when {
            (incomingTransfers.size == 1 && outgoingTransfers.isEmpty()) -> {
                val transfer = incomingTransfers.first()
                SolanaIncomingTransactionRecord(transaction, baseToken, source, transfer.address, transfer.value)
            }

            (incomingTransfers.isEmpty() && outgoingTransfers.size == 1) -> {
                val transfer = outgoingTransfers.first()
                SolanaOutgoingTransactionRecord(transaction, baseToken, source, transfer.address, transfer.value, transfer.address == userAddress)
            }

            else -> SolanaUnknownTransactionRecord(transaction, baseToken, source, incomingTransfers, outgoingTransfers)
        }
    }

}
