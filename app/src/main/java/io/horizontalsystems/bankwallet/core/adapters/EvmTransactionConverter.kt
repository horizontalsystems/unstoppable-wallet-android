package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractCreationDecoration
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.decorations.OneInchDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnknownDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapDecoration
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionConverter(
    private val coinManager: ICoinManager,
    private val evmKitWrapper: EvmKitWrapper,
    private val source: TransactionSource,
    private val baseCoin: PlatformCoin
) {

    private val evmKit: EthereumKit
        get() = evmKitWrapper.evmKit

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        return when (val decoration = fullTransaction.decoration) {
            is ContractCreationDecoration -> {
                ContractCreationTransactionRecord(transaction, baseCoin, source)
            }

            is IncomingDecoration -> {
                EvmIncomingTransactionRecord(transaction, baseCoin, source, decoration.from.eip55, baseCoinValue(decoration.value, false))
            }

            is OutgoingDecoration -> {
                EvmOutgoingTransactionRecord(transaction, baseCoin, source, decoration.to.eip55, baseCoinValue(decoration.value, true), decoration.sentToSelf)
            }

            is OutgoingEip20Decoration -> {
                EvmOutgoingTransactionRecord(transaction, baseCoin, source, decoration.to.eip55, getEip20Value(decoration.contractAddress, decoration.value, true), decoration.sentToSelf)
            }

            is ApproveEip20Decoration -> {
                ApproveTransactionRecord(transaction, baseCoin, source, decoration.spender.eip55, getEip20Value(decoration.contractAddress, decoration.value, false))
            }

            is SwapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseCoin, source,
                    decoration.contractAddress.eip55,
                    convertToAmount(decoration.tokenIn, decoration.amountIn, true),
                    convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    decoration.recipient != null
                )
            }

            is OneInchSwapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseCoin, source,
                    decoration.contractAddress.eip55,
                    SwapTransactionRecord.Amount.Exact(convertToTransactionValue(decoration.tokenIn, decoration.amountIn, true)),
                    convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    decoration.recipient != null
                )
            }

            is OneInchUnoswapDecoration -> {
                SwapTransactionRecord(
                    transaction, baseCoin, source,
                    decoration.contractAddress.eip55,
                    SwapTransactionRecord.Amount.Exact(convertToTransactionValue(decoration.tokenIn, decoration.amountIn, true)),
                    decoration.tokenOut?.let { convertToAmount(it, decoration.amountOut, false) },
                    false
                )
            }

            is OneInchUnknownDecoration -> {
                val address = evmKit.receiveAddress

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }

                val transferEventInstances = decoration.eventInstances.mapNotNull { it as TransferEventInstance }
                val incomingTransfers = transferEventInstances.filter { it.to == address && it.from != address }
                val outgoingTransfers = transferEventInstances.filter { it.from == address }

                return UnknownSwapTransactionRecord(
                    transaction, baseCoin, source,
                    baseCoinValue(decoration.value, true),
                    decoration.contractAddress.eip55,
                    getTransferEvents(internalTransactions),
                    getIncomingEip20Events(incomingTransfers),
                    getOutgoingEip20Events(outgoingTransfers)
                )
            }

            is UnknownTransactionDecoration -> {
                val address = evmKit.receiveAddress

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }

                val transferEventInstances = decoration.eventInstances.mapNotNull { it as TransferEventInstance }
                val incomingTransfers = transferEventInstances.filter { it.to == address && it.from != address }
                val outgoingTransfers = transferEventInstances.filter { it.from == address }

                when {
                    (transaction.from != address && internalTransactions.size == 1 && transferEventInstances.isEmpty()) -> {
                        val internalTransaction = internalTransactions.first()

                        return EvmIncomingTransactionRecord(
                            transaction, baseCoin, source,
                            internalTransaction.from.eip55,
                            baseCoinValue(internalTransaction.value, false),
                            true
                        )
                    }

                    (transaction.from != address && incomingTransfers.size == 1 && internalTransactions.isEmpty() && outgoingTransfers.isEmpty()) -> {
                        val transfer = incomingTransfers.first()

                        return EvmIncomingTransactionRecord(
                            transaction, baseCoin, source,
                            transfer.from.eip55,
                            getEip20Value(transfer.contractAddress, transfer.value, false)
                        )
                    }

                    (transaction.from != address && outgoingTransfers.size == 1 && internalTransactions.isEmpty() && incomingTransfers.isEmpty()) -> {
                        val transfer = outgoingTransfers.first()

                        return EvmOutgoingTransactionRecord(
                            transaction, baseCoin, source,
                            transfer.to.eip55,
                            getEip20Value(transfer.contractAddress, transfer.value, true),
                            transfer.to == address
                        )
                    }

                    else ->
                        return ContractCallTransactionRecord(
                            transaction, baseCoin, source,
                            transaction.to?.eip55,
                            null,
                            transaction.value?.let { baseCoinValue(it, true) },
                            getTransferEvents(internalTransactions),
                            getIncomingEip20Events(incomingTransfers),
                            getOutgoingEip20Events(outgoingTransfers)
                        )
                }

            }

            else -> {
                EvmTransactionRecord(transaction, baseCoin, source)
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

    private fun getEip20Value(tokenAddress: Address, amount: BigInteger, negative: Boolean): TransactionValue {
        val coinType = evmKitWrapper.blockchain.getEvm20CoinType(tokenAddress.hex)
        val platformCoin = coinManager.getPlatformCoin(coinType)

        return if (platformCoin != null) {
            TransactionValue.CoinValue(platformCoin, convertAmount(amount, platformCoin.decimals, negative))
        } else {
            TransactionValue.RawValue(value = amount)
        }
    }

    private fun convertToTransactionValue(token: SwapDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            SwapDecoration.Token.EvmCoin -> {
                baseCoinValue(amount, negative)
            }
            is SwapDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative)
            }
        }
    }

    private fun convertToTransactionValue(token: OneInchDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            OneInchDecoration.Token.EvmCoin -> {
                baseCoinValue(amount, negative)
            }
            is OneInchDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative)
            }
        }
    }

    private fun baseCoinValue(value: BigInteger, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, baseCoin.decimals, negative)

        return TransactionValue.CoinValue(baseCoin, amount)
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

    private fun getTransferEvents(internalTransactions: List<InternalTransaction>): List<EvmTransactionRecord.TransferEvent> {
        return internalTransactions.map { internalTransaction ->
            EvmTransactionRecord.TransferEvent(internalTransaction.from.eip55, baseCoinValue(internalTransaction.value, false))
        }
    }

    private fun getIncomingEip20Events(incomingTransfers: List<TransferEventInstance>): List<EvmTransactionRecord.TransferEvent> {
        return incomingTransfers.map { transfer ->
            EvmTransactionRecord.TransferEvent(transfer.from.eip55, getEip20Value(transfer.contractAddress, transfer.value, false))
        }
    }

    private fun getOutgoingEip20Events(outgoingTransfers: List<TransferEventInstance>): List<EvmTransactionRecord.TransferEvent> {
        return outgoingTransfers.map { transfer ->
            EvmTransactionRecord.TransferEvent(transfer.to.eip55, getEip20Value(transfer.contractAddress, transfer.value, true))
        }
    }

}
