package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.SolanaKitWrapper
import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.entities.transactionrecords.solana.SolanaTransactionRecord
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.solanakit.models.FullTransaction
import java.math.BigDecimal

class SolanaTransactionConverter(
    private val coinManager: ICoinManager,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val spamManager: SpamManager,
    solanaKitWrapper: SolanaKitWrapper
) {
    private val userAddress = solanaKitWrapper.solanaKit.receiveAddress

    fun transactionRecord(fullTransaction: FullTransaction): SolanaTransactionRecord {
        val transaction = fullTransaction.transaction
        val incomingSolanaTransfers = mutableListOf<SolanaTransactionRecord.SolanaTransfer>()
        val outgoingSolanaTransfers = mutableListOf<SolanaTransactionRecord.SolanaTransfer>()

        transaction.amount?.let {
            if (transaction.from == userAddress) {
                val transactionValue = TransactionValue.CoinValue(
                    baseToken,
                    it.multiply(BigDecimal.valueOf(-1)).movePointLeft(baseToken.decimals)
                )
                outgoingSolanaTransfers.add(
                    SolanaTransactionRecord.SolanaTransfer(
                        transaction.to,
                        transactionValue
                    )
                )
            } else if (transaction.to == userAddress) {
                val transactionValue =
                    TransactionValue.CoinValue(baseToken, it.movePointLeft(baseToken.decimals))
                incomingSolanaTransfers.add(
                    SolanaTransactionRecord.SolanaTransfer(
                        transaction.from,
                        transactionValue
                    )
                )
            } else {
            }
        }

        for (fullTokenTransfer in fullTransaction.tokenTransfers) {
            val tokenTransfer = fullTokenTransfer.tokenTransfer
            val mintAccount = fullTokenTransfer.mintAccount
            val query = TokenQuery(BlockchainType.Solana, TokenType.Spl(tokenTransfer.mintAddress))
            val token = coinManager.getToken(query)

            val transactionValue = when {
                token != null -> TransactionValue.CoinValue(
                    token,
                    tokenTransfer.amount.movePointLeft(token.decimals)
                )

                mintAccount.isNft -> TransactionValue.NftValue(
                    NftUid.Solana(mintAccount.address),
                    tokenTransfer.amount,
                    mintAccount.name,
                    mintAccount.symbol
                )

                else -> TransactionValue.RawValue(value = tokenTransfer.amount.toBigInteger())
            }

            if (tokenTransfer.incoming) {
                incomingSolanaTransfers.add(
                    SolanaTransactionRecord.SolanaTransfer(
                        fullTransaction.transaction.from,
                        transactionValue
                    )
                )
            } else {
                outgoingSolanaTransfers.add(
                    SolanaTransactionRecord.SolanaTransfer(
                        fullTransaction.transaction.to,
                        transactionValue
                    )
                )
            }
        }

        return when {
            (incomingSolanaTransfers.size == 1 && outgoingSolanaTransfers.isEmpty()) -> {
                val transfer = incomingSolanaTransfers.first()
                SolanaTransactionRecord(
                    transaction = transaction,
                    baseToken = baseToken,
                    source = source,
                    from = transfer.address,
                    value = transfer.value,
                    spam = spamManager.isSpam(
                        incomingEvents = incomingSolanaTransfers.map {
                            TransferEvent(it.address, it.value)
                        }, outgoingEvents = outgoingSolanaTransfers.map {
                            TransferEvent(it.address, it.value)
                        }),
                    transactionRecordType = TransactionRecordType.SOLANA_INCOMING
                )
            }

            (incomingSolanaTransfers.isEmpty() && outgoingSolanaTransfers.size == 1) -> {
                val transfer = outgoingSolanaTransfers.first()
                SolanaTransactionRecord(
                    transaction = transaction,
                    baseToken = baseToken,
                    source = source,
                    to = transfer.address,
                    value = transfer.value,
                    sentToSelf = transfer.address == userAddress,
                    transactionRecordType = TransactionRecordType.SOLANA_OUTGOING
                )
            }

            else -> SolanaTransactionRecord(
                transaction = transaction,
                baseToken = baseToken,
                source = source,
                incomingSolanaTransfers = incomingSolanaTransfers,
                outgoingSolanaTransfers = outgoingSolanaTransfers,
                spam = spamManager.isSpam(
                    incomingEvents = incomingSolanaTransfers.map {
                        TransferEvent(it.address, it.value)
                    }, outgoingEvents = outgoingSolanaTransfers.map {
                        TransferEvent(it.address, it.value)
                    }),
                transactionRecordType = TransactionRecordType.SOLANA_UNKNOWN
            )
        }
    }

}
