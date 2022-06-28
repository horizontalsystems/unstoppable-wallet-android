package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractCreationDecoration
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnknownDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import io.horizontalsystems.xxxkit.models.Token
import io.horizontalsystems.xxxkit.models.TokenQuery
import io.horizontalsystems.xxxkit.models.TokenType
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionConverter(
    private val coinManager: ICoinManager,
    private val evmKitWrapper: EvmKitWrapper,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    private val evmKit: EthereumKit
        get() = evmKitWrapper.evmKit

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        return when (val decoration = fullTransaction.decoration) {
            is ContractCreationDecoration -> {
                ContractCreationTransactionRecord(transaction, baseToken, source)
            }

            is IncomingDecoration -> {
                EvmIncomingTransactionRecord(transaction, baseToken, source, decoration.from.eip55, baseCoinValue(decoration.value, false))
            }

            is OutgoingDecoration -> {
                EvmOutgoingTransactionRecord(transaction, baseToken, source, decoration.to.eip55, baseCoinValue(decoration.value, true), decoration.sentToSelf)
            }

            is OutgoingEip20Decoration -> {
                EvmOutgoingTransactionRecord(transaction, baseToken, source, decoration.to.eip55, getEip20Value(decoration.contractAddress, decoration.value, true, decoration.tokenInfo), decoration.sentToSelf)
            }

            is ApproveEip20Decoration -> {
                ApproveTransactionRecord(transaction, baseToken, source, decoration.spender.eip55, getEip20Value(decoration.contractAddress, decoration.value, false))
            }

            is SwapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseToken, source,
                    decoration.contractAddress.eip55,
                    convertToAmount(decoration.tokenIn, decoration.amountIn, true),
                    convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    decoration.recipient?.eip55
                )
            }

            is OneInchSwapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseToken, source,
                    decoration.contractAddress.eip55,
                    SwapTransactionRecord.Amount.Exact(convertToTransactionValue(decoration.tokenIn, decoration.amountIn, true)),
                    convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    decoration.recipient?.eip55
                )
            }

            is OneInchUnoswapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseToken, source,
                    decoration.contractAddress.eip55,
                    SwapTransactionRecord.Amount.Exact(convertToTransactionValue(decoration.tokenIn, decoration.amountIn, true)),
                    decoration.tokenOut?.let { convertToAmount(it, decoration.amountOut, false) },
                    null
                )
            }

            is OneInchUnknownDecoration -> {
                return UnknownSwapTransactionRecord(
                    transaction, baseToken, source,
                    decoration.contractAddress.eip55,
                    decoration.tokenAmountIn?.let { convertToTransactionValue(it.token, it.value, true) },
                    decoration.tokenAmountOut?.let { convertToTransactionValue(it.token, it.value, true) }
                )
            }

            is UnknownTransactionDecoration -> {
                val address = evmKit.receiveAddress

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }
                val transferEventInstances = decoration.eventInstances.mapNotNull { it as? TransferEventInstance }
                val incomingTransfers = transferEventInstances.filter { it.to == address && it.from != address }
                val outgoingTransfers = transferEventInstances.filter { it.from == address }

                val contractAddress = transaction.to
                val value = transaction.value

                return if (transaction.from == address && contractAddress != null && value != null) {
                    ContractCallTransactionRecord(
                        transaction, baseToken, source,
                        contractAddress.eip55,
                        transaction.input?.let { evmLabelManager.methodLabel(it) },
                        getInternalEvents(internalTransactions) + getIncomingEvents(incomingTransfers),
                        getTransactionValueEvents(transaction) + getOutgoingEvents(outgoingTransfers)
                    )
                } else {
                    ExternalContractCallTransactionRecord(
                        transaction, baseToken, source,
                        getInternalEvents(internalTransactions) + getIncomingEvents(incomingTransfers),
                        getOutgoingEvents(outgoingTransfers)
                    )
                }

            }

            else -> {
                EvmTransactionRecord(transaction, baseToken, source)
            }

        }
    }

    private fun convertAmount(amount: BigInteger, decimal: Int, negative: Boolean): BigDecimal {
        var significandAmount = amount.toBigDecimal().movePointLeft(decimal).stripTrailingZeros()

        if (significandAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO
        }

        if (negative) {
            significandAmount = significandAmount.negate()
        }

        return significandAmount
    }

    private fun getEip20Value(tokenAddress: Address, amount: BigInteger, negative: Boolean, tokenInfo: TokenInfo? = null): TransactionValue {
        val query = TokenQuery(evmKitWrapper.blockchainType, TokenType.Eip20(tokenAddress.hex))
        val token = coinManager.getToken(query)

        return when {
            token != null -> {
                TransactionValue.CoinValue(token, convertAmount(amount, token.decimals, negative))
            }
            tokenInfo != null -> {
                TransactionValue.TokenValue("", tokenInfo.tokenName, tokenInfo.tokenSymbol, tokenInfo.tokenDecimal, convertAmount(amount, tokenInfo.tokenDecimal, negative))
            }
            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }

    private fun convertToTransactionValue(token: SwapDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            SwapDecoration.Token.EvmCoin -> {
                baseCoinValue(amount, negative)
            }
            is SwapDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative, token.tokenInfo)
            }
        }
    }

    private fun convertToTransactionValue(token: OneInchDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            OneInchDecoration.Token.EvmCoin -> {
                baseCoinValue(amount, negative)
            }
            is OneInchDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative, token.tokenInfo)
            }
        }
    }

    private fun baseCoinValue(value: BigInteger, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, baseToken.decimals, negative)

        return TransactionValue.CoinValue(baseToken, amount)
    }

    private fun convertToAmount(token: SwapDecoration.Token, amount: SwapDecoration.Amount, negative: Boolean): SwapTransactionRecord.Amount {
        return when (amount) {
            is SwapDecoration.Amount.Exact -> SwapTransactionRecord.Amount.Exact(convertToTransactionValue(token, amount.value, negative))
            is SwapDecoration.Amount.Extremum -> SwapTransactionRecord.Amount.Extremum(convertToTransactionValue(token, amount.value, negative))
        }
    }

    private fun convertToAmount(token: OneInchDecoration.Token, amount: OneInchDecoration.Amount, negative: Boolean): SwapTransactionRecord.Amount {
        return when (amount) {
            is OneInchDecoration.Amount.Exact -> SwapTransactionRecord.Amount.Exact(convertToTransactionValue(token, amount.value, negative))
            is OneInchDecoration.Amount.Extremum -> SwapTransactionRecord.Amount.Extremum(convertToTransactionValue(token, amount.value, negative))
        }
    }


    private fun getInternalEvents(internalTransactions: List<InternalTransaction>): List<EvmTransactionRecord.TransferEvent> {
        val events: MutableList<EvmTransactionRecord.TransferEvent> = mutableListOf()

        for (transaction in internalTransactions) {
            events.add(
                EvmTransactionRecord.TransferEvent(transaction.from.eip55, baseCoinValue(transaction.value, false))
            )
        }

        return events
    }

    private fun getTransactionValueEvents(transaction: Transaction): List<EvmTransactionRecord.TransferEvent> {
        val value = transaction.value
        if (value == null || value <= BigInteger.ZERO) return listOf()

        return listOf(
            EvmTransactionRecord.TransferEvent(transaction.to?.eip55, baseCoinValue(value, true))
        )
    }

    private fun getIncomingEvents(incomingTransfers: List<TransferEventInstance>): List<EvmTransactionRecord.TransferEvent> {
        val events: MutableList<EvmTransactionRecord.TransferEvent> = mutableListOf()

        for (transfer in incomingTransfers) {
            events.add(
                EvmTransactionRecord.TransferEvent(transfer.from.eip55, getEip20Value(transfer.contractAddress, transfer.value, false, transfer.tokenInfo))
            )
        }

        return events
    }

    private fun getOutgoingEvents(outgoingTransfers: List<TransferEventInstance>): List<EvmTransactionRecord.TransferEvent> {
        val events: MutableList<EvmTransactionRecord.TransferEvent> = mutableListOf()

        for (transfer in outgoingTransfers) {
            events.add(
                EvmTransactionRecord.TransferEvent(transfer.to.eip55, getEip20Value(transfer.contractAddress, transfer.value, true, transfer.tokenInfo))
            )
        }

        return events
    }

}
