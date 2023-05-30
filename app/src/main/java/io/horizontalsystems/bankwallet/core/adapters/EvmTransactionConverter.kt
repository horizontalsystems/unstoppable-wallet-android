package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmKitWrapper
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.tokenIconPlaceholder
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ContractCreationTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.EvmTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.SwapTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.UnknownSwapTransactionRecord
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
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.nftkit.decorations.OutgoingEip1155Decoration
import io.horizontalsystems.nftkit.decorations.OutgoingEip721Decoration
import io.horizontalsystems.nftkit.events.Eip1155TransferEventInstance
import io.horizontalsystems.nftkit.events.Eip721TransferEventInstance
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
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    private val evmKit: EthereumKit
        get() = evmKitWrapper.evmKit

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        val transactionRecord = when (val decoration = fullTransaction.decoration) {
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
                UnknownSwapTransactionRecord(
                    transaction, baseToken, source,
                    decoration.contractAddress.eip55,
                    decoration.tokenAmountIn?.let { convertToTransactionValue(it.token, it.value, true) },
                    decoration.tokenAmountOut?.let { convertToTransactionValue(it.token, it.value, true) }
                )
            }

            is OutgoingEip721Decoration -> {
                EvmOutgoingTransactionRecord(
                    transaction, baseToken, source,
                    decoration.to.eip55,
                    TransactionValue.NftValue(
                        nftUid = NftUid.Evm(source.blockchain.type, decoration.contractAddress.hex, decoration.tokenId.toString()),
                        value = BigDecimal.valueOf(-1),
                        tokenName = decoration.tokenInfo?.tokenName,
                        tokenSymbol = decoration.tokenInfo?.tokenSymbol
                    ),
                    decoration.sentToSelf
                )
            }

            is OutgoingEip1155Decoration -> {
                EvmOutgoingTransactionRecord(
                    transaction, baseToken, source,
                    decoration.to.eip55,
                    TransactionValue.NftValue(
                        nftUid = NftUid.Evm(source.blockchain.type, decoration.contractAddress.hex, decoration.tokenId.toString()),
                        value = convertAmount(decoration.value, 0, true),
                        tokenName = decoration.tokenInfo?.tokenName,
                        tokenSymbol = decoration.tokenInfo?.tokenSymbol
                    ),
                    decoration.sentToSelf
                )
            }

            is UnknownTransactionDecoration -> {
                val address = evmKit.receiveAddress

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }

                val eip20Transfers = decoration.eventInstances.mapNotNull { it as? TransferEventInstance }
                val incomingEip20Transfers = eip20Transfers.filter { it.to == address && it.from != address }
                val outgoingEip20Transfers = eip20Transfers.filter { it.from == address }

                val eip721Transfers = decoration.eventInstances.mapNotNull { it as? Eip721TransferEventInstance }
                val incomingEip721Transfers = eip721Transfers.filter { it.to == address && it.from != address }
                val outgoingEip721Transfers = eip721Transfers.filter { it.from == address }

                val eip1155Transfers = decoration.eventInstances.mapNotNull { it as? Eip1155TransferEventInstance }
                val incomingEip1155Transfers = eip1155Transfers.filter { it.to == address && it.from != address }
                val outgoingEip1155Transfers = eip1155Transfers.filter { it.from == address }

                val contractAddress = transaction.to
                val value = transaction.value

                when {
                    transaction.from == address && contractAddress != null && value != null -> {
                        ContractCallTransactionRecord(
                            transaction, baseToken, source,
                            contractAddress.eip55,
                            transaction.input?.let { evmLabelManager.methodLabel(it) },
                            getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers) +
                                    getIncomingEip721Events(incomingEip721Transfers) +
                                    getIncomingEip1155Events(incomingEip1155Transfers),
                            getTransactionValueEvents(transaction) +
                                    getOutgoingEip20Events(outgoingEip20Transfers) +
                                    getOutgoingEip721Events(outgoingEip721Transfers) +
                                    getOutgoingEip1155Events(outgoingEip1155Transfers)
                        )
                    }
                    transaction.from != address && transaction.to != address -> {
                        ExternalContractCallTransactionRecord(
                            transaction, baseToken, source,
                            getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers) +
                                    getIncomingEip721Events(incomingEip721Transfers) +
                                    getIncomingEip1155Events(incomingEip1155Transfers),
                            getOutgoingEip20Events(outgoingEip20Transfers) +
                                    getOutgoingEip721Events(outgoingEip721Transfers) +
                                    getOutgoingEip1155Events(outgoingEip1155Transfers)
                        )
                    }
                    else -> null
                }
            }
            else -> null
        }

        return transactionRecord ?: EvmTransactionRecord(
            transaction = transaction,
            baseToken = baseToken,
            source = source,
            foreignTransaction = transaction.from != evmKit.receiveAddress
        )
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
                TransactionValue.TokenValue(
                    tokenName = tokenInfo.tokenName,
                    tokenCode = tokenInfo.tokenSymbol,
                    tokenDecimals = tokenInfo.tokenDecimal,
                    value = convertAmount(amount, tokenInfo.tokenDecimal, negative),
                    coinIconPlaceholder = evmKitWrapper.blockchainType.tokenIconPlaceholder
                )
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


    private fun getInternalEvents(internalTransactions: List<InternalTransaction>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transaction in internalTransactions) {
            events.add(
                TransferEvent(transaction.from.eip55, baseCoinValue(transaction.value, false))
            )
        }

        return events
    }

    private fun getTransactionValueEvents(transaction: Transaction): List<TransferEvent> {
        val value = transaction.value
        if (value == null || value <= BigInteger.ZERO) return listOf()

        return listOf(
            TransferEvent(transaction.to?.eip55, baseCoinValue(value, true))
        )
    }

    private fun getIncomingEip20Events(incomingTransfers: List<TransferEventInstance>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transfer in incomingTransfers) {
            events.add(
                TransferEvent(
                    transfer.from.eip55,
                    getEip20Value(transfer.contractAddress, transfer.value, false, transfer.tokenInfo)
                )
            )
        }

        return events
    }

    private fun getOutgoingEip20Events(outgoingTransfers: List<TransferEventInstance>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transfer in outgoingTransfers) {
            events.add(
                TransferEvent(
                    transfer.to.eip55,
                    getEip20Value(transfer.contractAddress, transfer.value, true, transfer.tokenInfo)
                )
            )
        }

        return events
    }

    private fun getIncomingEip721Events(incomingTransfers: List<Eip721TransferEventInstance>): List<TransferEvent> =
        incomingTransfers.map { transfer ->
            TransferEvent(
                transfer.from.eip55,
                TransactionValue.NftValue(
                    nftUid = NftUid.Evm(source.blockchain.type, transfer.contractAddress.hex, transfer.tokenId.toString()),
                    value = BigDecimal.valueOf(1),
                    tokenName = transfer.tokenInfo?.tokenName,
                    tokenSymbol = transfer.tokenInfo?.tokenSymbol
                )
            )
        }

    private fun getOutgoingEip721Events(outgoingTransfers: List<Eip721TransferEventInstance>): List<TransferEvent> =
        outgoingTransfers.map { transfer ->
            TransferEvent(
                transfer.to.eip55,
                TransactionValue.NftValue(
                    nftUid = NftUid.Evm(source.blockchain.type, transfer.contractAddress.hex, transfer.tokenId.toString()),
                    value = BigDecimal.valueOf(-1),
                    tokenName = transfer.tokenInfo?.tokenName,
                    tokenSymbol = transfer.tokenInfo?.tokenSymbol
                )
            )
        }

    private fun getIncomingEip1155Events(incomingTransfers: List<Eip1155TransferEventInstance>): List<TransferEvent> =
        incomingTransfers.map { transfer ->
            TransferEvent(
                transfer.from.eip55,
                TransactionValue.NftValue(
                    nftUid = NftUid.Evm(source.blockchain.type, transfer.contractAddress.hex, transfer.tokenId.toString()),
                    value = convertAmount(transfer.value, 0, false),
                    tokenName = transfer.tokenInfo?.tokenName,
                    tokenSymbol = transfer.tokenInfo?.tokenSymbol
                )
            )
        }

    private fun getOutgoingEip1155Events(outgoingTransfers: List<Eip1155TransferEventInstance>): List<TransferEvent> =
        outgoingTransfers.map { transfer ->
            TransferEvent(
                transfer.to.eip55,
                TransactionValue.NftValue(
                    nftUid = NftUid.Evm(source.blockchain.type, transfer.contractAddress.hex, transfer.tokenId.toString()),
                    value = convertAmount(transfer.value, 0, true),
                    tokenName = transfer.tokenInfo?.tokenName,
                    tokenSymbol = transfer.tokenInfo?.tokenSymbol
                )
            )
        }

}
