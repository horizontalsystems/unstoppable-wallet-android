package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.RecognizedMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionConverter(
    private val coinManager: ICoinManager,
    private val evmKit: EthereumKit,
    private val source: TransactionSource,
    private val baseCoin: PlatformCoin
) {

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        val to =
            transaction.to ?: return ContractCreationTransactionRecord(fullTransaction, baseCoin, source)

        val methodDecoration = fullTransaction.mainDecoration

        val record: EvmTransactionRecord = if (methodDecoration != null) {
            when (fullTransaction.transaction.from) {
                evmKit.receiveAddress -> convertMyCall(methodDecoration, fullTransaction, to)
                else -> convertForeignCall(methodDecoration, fullTransaction, to)
            }
        } else {
            when {
                transaction.from == evmKit.receiveAddress -> {
                    val amount = convertAmount(transaction.value, baseCoin.decimals, true)

                    EvmOutgoingTransactionRecord(
                        fullTransaction = fullTransaction,
                        baseCoin = baseCoin,
                        value = TransactionValue.CoinValue(baseCoin, amount),
                        to = to.eip55,
                        sentToSelf = to == transaction.from,
                        source = source
                    )
                }
                to == evmKit.receiveAddress -> {
                    val amount = convertAmount(transaction.value, baseCoin.decimals, false)

                    EvmIncomingTransactionRecord(
                        fullTransaction = fullTransaction,
                        baseCoin = baseCoin,
                        value = TransactionValue.CoinValue(baseCoin, amount),
                        from = transaction.from.eip55,
                        source = source
                    )
                }
                else -> throw IllegalArgumentException()
            }
        }

        return record
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
        val coinType = when (evmKit.networkType) {
            EthereumKit.NetworkType.BscMainNet -> CoinType.Bep20(tokenAddress.hex)
            else -> CoinType.Erc20(tokenAddress.hex)
        }

        val platformCoin = coinManager.getPlatformCoin(coinType)

        return if (platformCoin != null) {
            TransactionValue.CoinValue(platformCoin, convertAmount(amount, platformCoin.decimals, negative))
        } else {
            TransactionValue.RawValue(value = amount)
        }
    }

    private fun convertToTransactionValue(token: SwapMethodDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            SwapMethodDecoration.Token.EvmCoin -> {
                val value = convertAmount(amount, baseCoin.decimals, negative)
                TransactionValue.CoinValue(baseCoin, value)
            }
            is SwapMethodDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative)
            }
        }
    }

    private fun convertToTransactionValue(token: OneInchMethodDecoration.Token, amount: BigInteger, negative: Boolean): TransactionValue {
        return when (token) {
            OneInchMethodDecoration.Token.EvmCoin -> {
                val value = convertAmount(amount, baseCoin.decimals, negative)
                TransactionValue.CoinValue(baseCoin, value)
            }
            is OneInchMethodDecoration.Token.Eip20 -> {
                getEip20Value(token.address, amount, negative)
            }
        }
    }

    private fun convertMyCall(
        methodDecoration: ContractMethodDecoration,
        fullTransaction: FullTransaction,
        to: Address
    ): EvmTransactionRecord {

        return when (methodDecoration) {
            is TransferMethodDecoration -> {
                EvmOutgoingTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    value = getEip20Value(to, methodDecoration.value, true),
                    to = methodDecoration.to.eip55,
                    sentToSelf = methodDecoration.to == fullTransaction.transaction.from,
                    source = source
                )
            }
            is ApproveMethodDecoration -> {
                ApproveTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    value = getEip20Value(to, methodDecoration.value, false),
                    spender = methodDecoration.spender.eip55,
                    source = source
                )
            }
            is SwapMethodDecoration -> {
                val resolvedAmountIn: BigInteger
                val resolvedAmountOut: BigInteger

                if (fullTransaction.isFailed()) {
                    resolvedAmountIn = BigInteger.ZERO
                    resolvedAmountOut = BigInteger.ZERO
                }
                else {
                    when (val trade = methodDecoration.trade) {
                        is SwapMethodDecoration.Trade.ExactIn -> {
                            resolvedAmountIn = trade.amountIn
                            resolvedAmountOut = trade.amountOut ?: trade.amountOutMin
                        }
                        is SwapMethodDecoration.Trade.ExactOut -> {
                            resolvedAmountIn = trade.amountIn ?: trade.amountInMax
                            resolvedAmountOut = trade.amountOut
                        }
                    }
                }

                SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    valueIn = convertToTransactionValue(methodDecoration.tokenIn, resolvedAmountIn, true),
                    valueOut = convertToTransactionValue(methodDecoration.tokenOut, resolvedAmountOut, false),
                    foreignRecipient = methodDecoration.to != evmKit.receiveAddress,
                    source = source
                )
            }
            is OneInchUnoswapMethodDecoration -> {
                val resolvedAmountIn: BigInteger
                val resolvedAmountOut: BigInteger

                if (fullTransaction.isFailed()) {
                    resolvedAmountIn = BigInteger.ZERO
                    resolvedAmountOut = BigInteger.ZERO
                } else {
                    resolvedAmountIn = methodDecoration.fromAmount
                    resolvedAmountOut = methodDecoration.toAmount ?: methodDecoration.toAmountMin
                }

                val valueIn = convertToTransactionValue(methodDecoration.fromToken, resolvedAmountIn, true)
                val valueOut = methodDecoration.toToken?.let { toToken ->
                    convertToTransactionValue(toToken, resolvedAmountOut, false)
                }

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    valueIn = valueIn,
                    valueOut = valueOut,
                    foreignRecipient = false,
                    source = source
                )
            }
            is OneInchSwapMethodDecoration -> {
                var tokenOut = methodDecoration.toToken

                var resolvedAmountIn = methodDecoration.fromAmount
                var resolvedAmountOut = methodDecoration.toAmount ?: methodDecoration.toAmountMin

                if (fullTransaction.isFailed()) {
                    resolvedAmountIn = BigInteger.ZERO
                    resolvedAmountOut = BigInteger.ZERO
                } else if (fullTransaction.receiptWithLogs != null && methodDecoration.toAmount == null) {
                    // Here we handle the case when transaction is completed, but reverted in smart contract.
                    // In that case, it transfers sent tokens/ETH back. So we should make a SwapTransactionRecord
                    // where token/ETH sent and token/ETH received are the same

                    for (event in fullTransaction.eventDecorations) {
                        if (event is TransferEventDecoration && event.to == evmKit.receiveAddress && event.value > BigInteger.ZERO) {
                            tokenOut = OneInchMethodDecoration.Token.Eip20(event.contractAddress)
                            resolvedAmountOut = event.value
                        }
                    }

                    var internalETHs = BigInteger.ZERO
                    for (tx in fullTransaction.internalTransactions) {
                        if (tx.to == evmKit.receiveAddress) {
                            internalETHs += tx.value
                        }
                    }

                    if (internalETHs > BigInteger.ZERO) {
                        tokenOut = OneInchMethodDecoration.Token.EvmCoin
                        resolvedAmountOut = internalETHs
                    }
                }

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    valueIn = convertToTransactionValue(methodDecoration.fromToken, resolvedAmountIn, true),
                    valueOut = convertToTransactionValue(tokenOut, resolvedAmountOut, false),
                    foreignRecipient = methodDecoration.recipient != evmKit.receiveAddress,
                    source = source
                )
            }
            is RecognizedMethodDecoration -> {
                ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimals, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction),
                    source
                )
            }
            is UnknownMethodDecoration -> {
                ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    null,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimals, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction),
                    source
                )
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun convertForeignCall(
        methodDecoration: ContractMethodDecoration,
        fullTransaction: FullTransaction,
        to: Address
    ): EvmTransactionRecord {
        when (methodDecoration) {
            is TransferMethodDecoration -> {
                if (methodDecoration.to == evmKit.receiveAddress) {
                    return EvmIncomingTransactionRecord(
                        fullTransaction = fullTransaction,
                        baseCoin = baseCoin,
                        value = getEip20Value(to, methodDecoration.value, false),
                        from = methodDecoration.to.eip55,
                        foreignTransaction = true,
                        source = source
                    )
                }
            }
            is RecognizedMethodDecoration -> {
                return ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimals, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction),
                    source
                )
            }
            is UnknownMethodDecoration -> {
                return ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    null,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimals, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction),
                    source
                )
            }
        }

        throw IllegalArgumentException()
    }

    private fun getInternalTransactions(fullTransaction: FullTransaction) : List<AddressTransactionValue> {
        return fullTransaction.internalTransactions.mapNotNull { internalTransaction ->
            if (internalTransaction.to == evmKit.receiveAddress) {
                val amount = convertAmount(internalTransaction.value, baseCoin.decimals, false)
                AddressTransactionValue(internalTransaction.from.eip55, TransactionValue.CoinValue(baseCoin, amount))
            } else {
                null
            }
        }
    }

    private fun getIncomingEip20Events(fullTransaction: FullTransaction) : List<AddressTransactionValue> {
        return fullTransaction.eventDecorations.mapNotNull { event ->
            if (event is TransferEventDecoration && event.to == evmKit.receiveAddress) {
                val value = getEip20Value(event.contractAddress, event.value, false)
                AddressTransactionValue(event.from.eip55, value)
            } else {
                null
            }
        }
    }

    private fun getOutgoingEip20Events(fullTransaction: FullTransaction) : List<AddressTransactionValue> {
        return fullTransaction.eventDecorations.mapNotNull { event ->
            if (event is TransferEventDecoration && event.from == evmKit.receiveAddress) {
                val value = getEip20Value(event.contractAddress, event.value, true)
                return@mapNotNull AddressTransactionValue(event.to.eip55, value)
            } else {
                null
            }
        }
    }
}
