package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.erc20kit.decorations.ApproveMethodDecoration
import io.horizontalsystems.erc20kit.decorations.TransferEventDecoration
import io.horizontalsystems.erc20kit.decorations.TransferMethodDecoration
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.decorations.ContractMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.RecognizedMethodDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownMethodDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionConverter(private val coinManager: ICoinManager, private val evmKit: EthereumKit) {

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        val to = transaction.to ?: return ContractCreationTransactionRecord(fullTransaction)

        val baseCoin: Coin = when (evmKit.networkType) {
            EthereumKit.NetworkType.BscMainNet -> getCoin(CoinType.BinanceSmartChain)
            else -> getCoin(CoinType.Ethereum)
        }

        val methodDecoration = fullTransaction.mainDecoration

        val record: EvmTransactionRecord = if (methodDecoration != null) {
            when (fullTransaction.transaction.from) {
                evmKit.receiveAddress -> convertMyCall(methodDecoration, fullTransaction, to)
                else -> convertForeignCall(methodDecoration, fullTransaction, to)
            }
        } else {
            when {
                transaction.from == evmKit.receiveAddress -> EvmOutgoingTransactionRecord(
                        fullTransaction = fullTransaction,
                        amount = convertAmount(transaction.value, baseCoin.decimal, true),
                        to = to.eip55,
                        token = baseCoin,
                        sentToSelf = to == transaction.from
                )
                to == evmKit.receiveAddress -> EvmIncomingTransactionRecord(
                        fullTransaction = fullTransaction,
                        amount = convertAmount(transaction.value, baseCoin.decimal, false),
                        from = transaction.from.eip55,
                        token = baseCoin
                )
                else -> throw IllegalArgumentException()
            }
        }

        if (record is ContractCallTransactionRecord) {
            fullTransaction.internalTransactions.forEach { internalTransaction ->
                if (internalTransaction.to == evmKit.receiveAddress) {
                    val amount = convertAmount(internalTransaction.value, baseCoin.decimal, false)
                    record.incomingInternalETHs.add(Pair(internalTransaction.from.eip55, amount))
                }
            }
            fullTransaction.eventDecorations.forEach { event ->
                if (event is TransferEventDecoration) {
                    val token = getEip20Coin(event.contractAddress)
                    if (event.from == evmKit.receiveAddress) {
                        val amount = convertAmount(event.value, token.decimal, true)
                        record.outgoingEip20Events.add(Pair(event.to.eip55, amount))
                    } else if (event.to == evmKit.receiveAddress) {
                        val amount = convertAmount(event.value, token.decimal, false)
                        record.incomingEip20Events.add(Pair(event.from.eip55, amount))
                    }
                }
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

    private fun getCoin(coinType: CoinType): Coin {
        return coinManager.getCoin(coinType) ?: Coin(title = "", code = "", decimal = 18, type = coinType)
    }

    private fun getEip20Coin(tokenAddress: Address): Coin {
        val coinType = when (evmKit.networkType) {
            EthereumKit.NetworkType.BscMainNet -> CoinType.Bep20(tokenAddress.hex)
            else -> CoinType.Erc20(tokenAddress.hex)
        }

        return getCoin(coinType)
    }

    private fun convertToCoin(token: SwapMethodDecoration.Token): Coin {
        return when (token) {
            SwapMethodDecoration.Token.EvmCoin -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.BscMainNet -> getCoin(CoinType.BinanceSmartChain)
                    else -> getCoin(CoinType.Ethereum)
                }
            }
            is SwapMethodDecoration.Token.Eip20Coin -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.BscMainNet -> getCoin(CoinType.Bep20(token.address.hex))
                    else -> getCoin(CoinType.Erc20(token.address.hex))
                }
            }
        }
    }

    private fun convertMyCall(methodDecoration: ContractMethodDecoration, fullTransaction: FullTransaction, to: Address): EvmTransactionRecord {

        return when (methodDecoration) {
            is TransferMethodDecoration -> {
                val token = getEip20Coin(to)
                EvmOutgoingTransactionRecord(
                        fullTransaction = fullTransaction,
                        amount = convertAmount(methodDecoration.value, token.decimal, true),
                        to = methodDecoration.to.eip55,
                        token = token,
                        sentToSelf = methodDecoration.to == fullTransaction.transaction.from
                )
            }
            is ApproveMethodDecoration -> {
                val token = getEip20Coin(to)
                ApproveTransactionRecord(
                        fullTransaction = fullTransaction,
                        amount = convertAmount(methodDecoration.value, token.decimal, false),
                        spender = methodDecoration.spender.eip55,
                        token = token,
                )
            }
            is SwapMethodDecoration -> {
                val resolvedAmountIn: BigInteger
                val resolvedAmountOut: BigInteger

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

                val tokenIn = convertToCoin(methodDecoration.tokenIn)
                val tokenOut = convertToCoin(methodDecoration.tokenOut)

                SwapTransactionRecord(
                        fullTransaction = fullTransaction,
                        exchangeAddress = to.eip55,
                        tokenIn = tokenIn,
                        tokenOut = tokenOut,
                        amountIn = convertAmount(resolvedAmountIn, tokenIn.decimal, true),
                        amountOut = convertAmount(resolvedAmountOut, tokenOut.decimal, false),
                        foreignRecipient = methodDecoration.to != evmKit.receiveAddress
                )
            }
            is RecognizedMethodDecoration -> {
                ContractCallTransactionRecord(fullTransaction, to.eip55, methodDecoration.method)
            }
            is UnknownMethodDecoration -> {
                ContractCallTransactionRecord(fullTransaction, to.eip55, null)
            }
            else -> throw IllegalArgumentException()
        }
    }

    private fun convertForeignCall(methodDecoration: ContractMethodDecoration, fullTransaction: FullTransaction, to: Address): EvmTransactionRecord {
        when (methodDecoration) {
            is TransferMethodDecoration -> {
                if (methodDecoration.to == evmKit.receiveAddress) {
                    val token = getEip20Coin(to)
                    return EvmIncomingTransactionRecord(
                            fullTransaction = fullTransaction,
                            amount = convertAmount(methodDecoration.value, token.decimal, false),
                            from = methodDecoration.to.eip55,
                            token = token
                    )
                }
            }
            is SwapMethodDecoration -> {
                if (methodDecoration.to == evmKit.receiveAddress) {
                    val resolvedAmountOut: BigInteger = when (val trade = methodDecoration.trade) {
                        is SwapMethodDecoration.Trade.ExactIn -> trade.amountOut ?: trade.amountOutMin
                        is SwapMethodDecoration.Trade.ExactOut -> trade.amountOut
                    }

                    val token = convertToCoin(methodDecoration.tokenOut)
                    return EvmIncomingTransactionRecord(
                            fullTransaction = fullTransaction,
                            amount = convertAmount(resolvedAmountOut, token.decimal, false),
                            from = methodDecoration.to.eip55,
                            token = token
                    )
                }
            }
            is RecognizedMethodDecoration -> {
                return ContractCallTransactionRecord(fullTransaction, to.eip55, methodDecoration.method)
            }
            is UnknownMethodDecoration -> {
                return ContractCallTransactionRecord(fullTransaction, to.eip55, null)
            }
        }

        throw IllegalArgumentException()
    }
}
