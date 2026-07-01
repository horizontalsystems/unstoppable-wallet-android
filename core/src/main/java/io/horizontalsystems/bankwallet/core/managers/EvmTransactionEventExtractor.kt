package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Extracts transfer events from EVM transactions for spam detection.
 */
class EvmTransactionEventExtractor {

    /**
     * Extract outgoing transaction info from EVM FullTransaction.
     */
    fun extractOutgoingInfo(
        fullTx: FullTransaction,
        userAddress: Address
    ): PoisoningScorer.OutgoingTxInfo? {
        val tx = fullTx.transaction
        return when (val decoration = fullTx.decoration) {
            is OutgoingDecoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is OutgoingEip20Decoration -> {
                PoisoningScorer.OutgoingTxInfo(decoration.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
            }
            is UnknownTransactionDecoration -> {
                if (tx.from == userAddress) {
                    decoration.eventInstances
                        .mapNotNull { it as? TransferEventInstance }
                        .firstOrNull { it.from == userAddress }?.let { transfer ->
                            PoisoningScorer.OutgoingTxInfo(transfer.to.eip55, tx.timestamp, tx.blockNumber?.toInt())
                        }
                } else null
            }
            else -> null
        }
    }

    /**
     * Extract incoming events from EVM FullTransaction.
     */
    fun extractIncomingEvents(
        fullTx: FullTransaction,
        userAddress: Address,
        baseToken: Token,
        blockchainType: BlockchainType
    ): List<TransferEvent> {
        return when (val decoration = fullTx.decoration) {
            is IncomingDecoration -> {
                val value = convertAmount(decoration.value, baseToken.decimals)
                listOf(TransferEvent(decoration.from.eip55, TransactionValue.CoinValue(baseToken, value)))
            }
            is UnknownTransactionDecoration -> {
                decoration.eventInstances
                    .mapNotNull { it as? TransferEventInstance }
                    .filter { it.to == userAddress || it.from == userAddress }
                    .map { transfer ->
                        val tokenValue = getEip20Value(transfer.contractAddress, transfer.value, blockchainType, transfer.tokenInfo)
                        if (transfer.from == userAddress) {
                            TransferEvent(transfer.to.eip55, tokenValue)
                        } else {
                            TransferEvent(transfer.from.eip55, tokenValue)
                        }
                    }
            }
            else -> emptyList()
        }
    }

    /**
     * Get transaction value for ERC20 transfer, matching EvmTransactionConverter logic.
     * Priority: known token (CoinValue) > tokenInfo (TokenValue) > raw value (RawValue)
     */
    private fun getEip20Value(
        tokenAddress: Address,
        amount: BigInteger,
        blockchainType: BlockchainType,
        tokenInfo: TokenInfo?
    ): TransactionValue {
        val query = TokenQuery(blockchainType, TokenType.Eip20(tokenAddress.hex))
        val token = App.coinManager.getToken(query)

        return when {
            token != null -> {
                TransactionValue.CoinValue(token, convertAmount(amount, token.decimals))
            }
            tokenInfo != null -> {
                TransactionValue.TokenValue(
                    tokenName = tokenInfo.tokenName,
                    tokenCode = tokenInfo.tokenSymbol,
                    tokenDecimals = tokenInfo.tokenDecimal,
                    value = convertAmount(amount, tokenInfo.tokenDecimal)
                )
            }
            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }

    private fun convertAmount(amount: BigInteger, decimals: Int): BigDecimal {
        val result = amount.toBigDecimal().movePointLeft(decimals).stripTrailingZeros()
        return if (result.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO else result
    }
}