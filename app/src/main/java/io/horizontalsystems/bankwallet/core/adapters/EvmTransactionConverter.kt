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

                val tokenIn = convertToCoin(methodDecoration.tokenIn)
                val tokenOut = convertToCoin(methodDecoration.tokenOut)

                SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = convertAmount(resolvedAmountIn, tokenIn.decimal, true),
                    amountOut = convertAmount(resolvedAmountOut, tokenOut.decimal, false),
                    foreignRecipient = methodDecoration.to != evmKit.receiveAddress
                )
            }
            is OneInchUnoswapMethodDecoration -> {
                val resolvedFromAmount: BigDecimal
                val resolvedToAmount: BigDecimal?

                val tokenIn = convertToCoin(methodDecoration.fromToken)
                val tokenOut = methodDecoration.toToken?.let { convertToCoin(it) }

                if (fullTransaction.isFailed()) {
                    resolvedFromAmount = BigDecimal.ZERO
                    resolvedToAmount = BigDecimal.ZERO
                } else {
                    resolvedFromAmount = convertAmount(
                        methodDecoration.fromAmount,
                        tokenIn.decimal,
                        true
                    )
                    resolvedToAmount = tokenOut?.let {
                        convertAmount(
                            methodDecoration.toAmount ?: methodDecoration.toAmountMin,
                            tokenOut.decimal,
                            false
                        )
                    }
                }

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = resolvedFromAmount,
                    amountOut = resolvedToAmount,
                    foreignRecipient = false
                )
            }
            is OneInchSwapMethodDecoration -> {
                val tokenIn = convertToCoin(methodDecoration.fromToken)
                var tokenOut = convertToCoin(methodDecoration.toToken)

                var resolvedAmountIn = convertAmount(methodDecoration.fromAmount, tokenIn.decimal, true)
                var resolvedAmountOut = convertAmount( methodDecoration.toAmount ?: methodDecoration.toAmountMin, tokenOut.decimal, false)

                if (fullTransaction.isFailed()) {
                    resolvedAmountIn = BigDecimal.ZERO
                    resolvedAmountOut = BigDecimal.ZERO
                } else if (fullTransaction.receiptWithLogs != null && methodDecoration.toAmount == null) {
                    // transaction can be reverted

                    for(event in getIncomingEip20Events(fullTransaction)) {
                        if (event.second.value > BigDecimal.ZERO) {
                            tokenOut = event.second.coin
                            resolvedAmountOut = event.second.value
                        }
                    }

                    var internalETHs = BigDecimal.ZERO
                    for(tx in getInternalTransactions(fullTransaction)) {
                        internalETHs += tx.second.value
                    }

                    if (internalETHs > BigDecimal.ZERO) {
                        tokenOut = baseCoin
                        resolvedAmountOut = internalETHs
                    }
                }

                return SwapTransactionRecord(
                    fullTransaction = fullTransaction,
                    baseCoin = baseCoin,
                    exchangeAddress = to.eip55,
                    tokenIn = tokenIn,
                    tokenOut = tokenOut,
                    amountIn = resolvedAmountIn,
                    amountOut = resolvedAmountOut,
                    foreignRecipient = methodDecoration.recipient != evmKit.receiveAddress
                )
            }
            is RecognizedMethodDecoration -> {
                ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimal, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction)
                )
            }
            is UnknownMethodDecoration -> {
                ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    null,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimal, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction)
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
                    val token = getEip20Coin(to)
                    return EvmIncomingTransactionRecord(
                        fullTransaction = fullTransaction,
                        baseCoin = baseCoin,
                        amount = convertAmount(methodDecoration.value, token.decimal, false),
                        from = methodDecoration.to.eip55,
                        token = token,
                        foreignTransaction = true
                    )
                }
            }
            is RecognizedMethodDecoration -> {
                return ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    methodDecoration.method,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimal, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction)
                )
            }
            is UnknownMethodDecoration -> {
                return ContractCallTransactionRecord(
                    fullTransaction,
                    baseCoin,
                    to.eip55,
                    null,
                    convertAmount(fullTransaction.transaction.value, baseCoin.decimal, true),
                    getInternalTransactions(fullTransaction),
                    getIncomingEip20Events(fullTransaction),
                    getOutgoingEip20Events(fullTransaction)
                )
            }
        }

        throw IllegalArgumentException()
    }

    private fun getInternalTransactions(fullTransaction: FullTransaction) : List<Pair<String, CoinValue>> {
        return fullTransaction.internalTransactions.mapNotNull { internalTransaction ->
            if (internalTransaction.to != evmKit.receiveAddress){
                return@mapNotNull null
            }

            val amount = convertAmount(internalTransaction.value, baseCoin.decimal, false)
            Pair(internalTransaction.from.eip55, CoinValue(baseCoin, amount))
        }
    }

    private fun getIncomingEip20Events(fullTransaction: FullTransaction) : List<Pair<String, CoinValue>> {
        return fullTransaction.eventDecorations.mapNotNull { event ->
            (event as? TransferEventDecoration)?.let { decoration ->
                if (decoration.to == evmKit.receiveAddress){
                    val token = getEip20Coin(decoration.contractAddress)
                    val amount = convertAmount(decoration.value, token.decimal, false)
                    return@mapNotNull Pair(decoration.from.eip55, CoinValue(token, amount))
                }
            }

            return@mapNotNull null
        }
    }

    private fun getOutgoingEip20Events(fullTransaction: FullTransaction) : List<Pair<String, CoinValue>> {
        return fullTransaction.eventDecorations.mapNotNull { event ->
            (event as? TransferEventDecoration)?.let { decoration ->
                if (decoration.from == evmKit.receiveAddress){
                    val token = getEip20Coin(decoration.contractAddress)
                    val amount = convertAmount(decoration.value, token.decimal, true)
                    return@mapNotNull Pair(decoration.to.eip55, CoinValue(token, amount))
                }
            }

            return@mapNotNull null
        }
    }
}
