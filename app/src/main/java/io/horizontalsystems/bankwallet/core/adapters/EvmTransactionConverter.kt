package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.CoinValue
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
import io.horizontalsystems.oneinchkit.decorations.OneInchMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchSwapMethodDecoration
import io.horizontalsystems.oneinchkit.decorations.OneInchUnoswapMethodDecoration
import io.horizontalsystems.uniswapkit.decorations.SwapMethodDecoration
import java.math.BigDecimal
import java.math.BigInteger

class EvmTransactionConverter(
    private val coinManager: ICoinManager,
    private val evmKit: EthereumKit
) {

    val baseCoin: Coin = when (evmKit.networkType) {
        EthereumKit.NetworkType.BscMainNet -> coinManager.getCoinOrStub(CoinType.BinanceSmartChain)
        else -> coinManager.getCoinOrStub(CoinType.Ethereum)
    }

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        val to =
            transaction.to ?: return ContractCreationTransactionRecord(fullTransaction, baseCoin)

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
                    baseCoin = baseCoin,
                    amount = convertAmount(transaction.value, baseCoin.decimal, true),
                    to = to.eip55,
                    token = baseCoin,
                    sentToSelf = to == transaction.from
                )
                to == evmKit.receiveAddress -> EvmIncomingTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
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
                    record.incomingInternalETHs.add(
                        Pair(
                            internalTransaction.from.eip55,
                            CoinValue(baseCoin, amount)
                        )
                    )
                }
            }
            fullTransaction.eventDecorations.forEach { event ->
                if (event is TransferEventDecoration) {
                    val token = getEip20Coin(event.contractAddress)
                    if (event.from == evmKit.receiveAddress) {
                        val amount = convertAmount(event.value, token.decimal, true)
                        record.outgoingEip20Events.add(
                            Pair(
                                event.to.eip55,
                                CoinValue(baseCoin, amount)
                            )
                        )
                    } else if (event.to == evmKit.receiveAddress) {
                        val amount = convertAmount(event.value, token.decimal, false)
                        record.incomingEip20Events.add(
                            Pair(
                                event.from.eip55,
                                CoinValue(baseCoin, amount)
                            )
                        )
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

    private fun getEip20Coin(tokenAddress: Address): Coin {
        val coinType = when (evmKit.networkType) {
            EthereumKit.NetworkType.BscMainNet -> CoinType.Bep20(tokenAddress.hex)
            else -> CoinType.Erc20(tokenAddress.hex)
        }

        return coinManager.getCoinOrStub(coinType)
    }

    private fun convertToCoin(token: SwapMethodDecoration.Token): Coin {
        return when (token) {
            SwapMethodDecoration.Token.EvmCoin -> {
                baseCoin
            }
            is SwapMethodDecoration.Token.Eip20Coin -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.BscMainNet -> coinManager.getCoinOrStub(
                        CoinType.Bep20(
                            token.address.hex
                        )
                    )
                    else -> coinManager.getCoinOrStub(CoinType.Erc20(token.address.hex))
                }
            }
        }
    }

    private fun convertToCoin(token: OneInchMethodDecoration.Token): Coin {
        return when (token) {
            OneInchMethodDecoration.Token.EvmCoin -> {
                baseCoin
            }
            is OneInchMethodDecoration.Token.Eip20 -> {
                when (evmKit.networkType) {
                    EthereumKit.NetworkType.BscMainNet -> coinManager.getCoinOrStub(
                        CoinType.Bep20(
                            token.address.hex
                        )
                    )
                    else -> coinManager.getCoinOrStub(CoinType.Erc20(token.address.hex))
                }
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
                val token = getEip20Coin(to)
                EvmOutgoingTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
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
                    baseCoin = baseCoin,
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
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = convertAmount(resolvedAmountIn, tokenIn.decimal, true),
                    amountOut = if (methodDecoration.to == evmKit.receiveAddress) convertAmount(
                        resolvedAmountOut,
                        tokenOut.decimal,
                        false
                    ) else null
                )
            }
            is OneInchUnoswapMethodDecoration -> {
                val tokenIn = convertToCoin(methodDecoration.fromToken)
                val tokenOut = methodDecoration.toToken?.let { convertToCoin(it) }

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = convertAmount(
                        methodDecoration.fromAmount,
                        tokenIn.decimal,
                        true
                    ),
                    amountOut = tokenOut?.let {
                        convertAmount(
                            methodDecoration.toAmount,
                            tokenOut.decimal,
                            false
                        )
                    }
                )
            }
            is OneInchSwapMethodDecoration -> {
                val tokenIn = convertToCoin(methodDecoration.fromToken)
                val tokenOut = convertToCoin(methodDecoration.toToken)

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = convertAmount(
                        methodDecoration.fromAmount,
                        tokenIn.decimal,
                        true
                    ),
                    amountOut = convertAmount(
                        methodDecoration.toAmount,
                        tokenOut.decimal,
                        false
                    )
                )
            }
            is RecognizedMethodDecoration -> {
                ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method
                )
            }
            is UnknownMethodDecoration -> {
                ContractCallTransactionRecord(fullTransaction, baseCoin, to.eip55, null)
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
                    val token = getEip20Coin(to)
                    return EvmIncomingTransactionRecord(
                        fullTransaction = fullTransaction,
                        baseCoin = baseCoin,
                        amount = convertAmount(methodDecoration.value, token.decimal, false),
                        from = methodDecoration.to.eip55,
                        token = token
                    )
                }
            }
            is RecognizedMethodDecoration -> {
                return ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method
                )
            }
            is UnknownMethodDecoration -> {
                return ContractCallTransactionRecord(fullTransaction, baseCoin, to.eip55, null)
            }
        }

        throw IllegalArgumentException()
    }
}
