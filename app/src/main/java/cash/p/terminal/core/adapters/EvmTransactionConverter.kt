package cash.p.terminal.core.adapters

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmLabelManager
import cash.p.terminal.core.managers.SpamManager
import cash.p.terminal.core.tokenIconPlaceholder
import cash.p.terminal.data.repository.EvmTransactionRepository
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.erc20kit.decorations.ApproveEip20Decoration
import io.horizontalsystems.erc20kit.decorations.OutgoingEip20Decoration
import io.horizontalsystems.erc20kit.events.TokenInfo
import io.horizontalsystems.erc20kit.events.TransferEventInstance
import io.horizontalsystems.ethereumkit.decorations.ContractCreationDecoration
import io.horizontalsystems.ethereumkit.decorations.IncomingDecoration
import io.horizontalsystems.ethereumkit.decorations.OutgoingDecoration
import io.horizontalsystems.ethereumkit.decorations.UnknownTransactionDecoration
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.FullTransaction
import io.horizontalsystems.ethereumkit.models.InternalTransaction
import io.horizontalsystems.ethereumkit.models.Transaction
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

internal class EvmTransactionConverter(
    private val coinManager: ICoinManager,
    private val evmTransactionRepository: EvmTransactionRepository,
    private val source: TransactionSource,
    private val spamManager: SpamManager,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {

    fun transactionRecord(fullTransaction: FullTransaction): EvmTransactionRecord {
        val transaction = fullTransaction.transaction

        val transactionRecord = when (val decoration = fullTransaction.decoration) {
            is ContractCreationDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction,
                    token = baseToken,
                    source = source,
                    transactionRecordType = TransactionRecordType.EVM_CONTRACT_CREATION
                )
            }

            is IncomingDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction,
                    token = baseToken,
                    source = source,
                    spamManager = spamManager,
                    from = decoration.from.eip55,
                    value = baseCoinValue(decoration.value, false),
                    transactionRecordType = TransactionRecordType.EVM_INCOMING
                )
            }

            is OutgoingDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction,
                    token = baseToken,
                    source = source,
                    to = decoration.to.eip55,
                    value = baseCoinValue(decoration.value, true),
                    sentToSelf = decoration.sentToSelf,
                    transactionRecordType = TransactionRecordType.EVM_OUTGOING
                )
            }

            is OutgoingEip20Decoration -> {
                EvmTransactionRecord(
                    transaction = transaction,
                    token = baseToken,
                    source = source,
                    to = decoration.to.eip55,
                    value = getEip20Value(
                        decoration.contractAddress,
                        decoration.value,
                        true,
                        decoration.tokenInfo
                    ),
                    sentToSelf = decoration.sentToSelf,
                    transactionRecordType = TransactionRecordType.EVM_OUTGOING
                )
            }

            is ApproveEip20Decoration -> {
                EvmTransactionRecord(
                    transaction = transaction,
                    token = baseToken,
                    source = source,
                    spender = decoration.spender.eip55,
                    value = getEip20Value(decoration.contractAddress, decoration.value, false),
                    transactionRecordType = TransactionRecordType.EVM_APPROVE
                )
            }

            is SwapDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    exchangeAddress = decoration.contractAddress.eip55,
                    amountIn = convertToAmount(decoration.tokenIn, decoration.amountIn, true),
                    amountOut = convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    recipient = decoration.recipient?.eip55,
                    transactionRecordType = TransactionRecordType.EVM_SWAP
                )
            }

            is OneInchSwapDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    exchangeAddress = decoration.contractAddress.eip55,
                    amountIn = EvmTransactionRecord.Amount.Exact(
                        convertToTransactionValue(
                            decoration.tokenIn,
                            decoration.amountIn,
                            true
                        )
                    ),
                    amountOut = convertToAmount(decoration.tokenOut, decoration.amountOut, false),
                    recipient = decoration.recipient?.eip55,
                    transactionRecordType = TransactionRecordType.EVM_SWAP
                )
            }

            is OneInchUnoswapDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    exchangeAddress = decoration.contractAddress.eip55,
                    amountIn = EvmTransactionRecord.Amount.Exact(
                        convertToTransactionValue(
                            decoration.tokenIn,
                            decoration.amountIn,
                            true
                        )
                    ),
                    amountOut = decoration.tokenOut?.let {
                        convertToAmount(
                            it,
                            decoration.amountOut,
                            false
                        )
                    },
                    recipient = null,
                    transactionRecordType = TransactionRecordType.EVM_SWAP
                )
            }

            is OneInchUnknownDecoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    exchangeAddress = decoration.contractAddress.eip55,
                    valueIn = decoration.tokenAmountIn?.let {
                        convertToTransactionValue(
                            it.token,
                            it.value,
                            true
                        )
                    },
                    valueOut = decoration.tokenAmountOut?.let {
                        convertToTransactionValue(
                            it.token,
                            it.value,
                            true
                        )
                    },
                    transactionRecordType = TransactionRecordType.EVM_UNKNOWN_SWAP
                )
            }

            is OutgoingEip721Decoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    to = decoration.to.eip55,
                    value = TransactionValue.NftValue(
                        nftUid = NftUid.Evm(
                            source.blockchain.type,
                            decoration.contractAddress.hex,
                            decoration.tokenId.toString()
                        ),
                        value = BigDecimal.valueOf(-1),
                        tokenName = decoration.tokenInfo?.tokenName,
                        tokenSymbol = decoration.tokenInfo?.tokenSymbol
                    ),
                    sentToSelf = decoration.sentToSelf,
                    transactionRecordType = TransactionRecordType.EVM_OUTGOING
                )
            }

            is OutgoingEip1155Decoration -> {
                EvmTransactionRecord(
                    transaction = transaction, token = baseToken, source = source,
                    to = decoration.to.eip55,
                    value = TransactionValue.NftValue(
                        nftUid = NftUid.Evm(
                            source.blockchain.type,
                            decoration.contractAddress.hex,
                            decoration.tokenId.toString()
                        ),
                        value = convertAmount(decoration.value, 0, true),
                        tokenName = decoration.tokenInfo?.tokenName,
                        tokenSymbol = decoration.tokenInfo?.tokenSymbol
                    ),
                    sentToSelf = decoration.sentToSelf,
                    transactionRecordType = TransactionRecordType.EVM_OUTGOING
                )
            }

            is UnknownTransactionDecoration -> {
                val address = evmTransactionRepository.receiveAddress

                val internalTransactions =
                    decoration.internalTransactions.filter { it.to == address }

                val eip20Transfers =
                    decoration.eventInstances.mapNotNull { it as? TransferEventInstance }
                val incomingEip20Transfers =
                    eip20Transfers.filter { it.to == address && it.from != address }
                val outgoingEip20Transfers = eip20Transfers.filter { it.from == address }

                val eip721Transfers =
                    decoration.eventInstances.mapNotNull { it as? Eip721TransferEventInstance }
                val incomingEip721Transfers =
                    eip721Transfers.filter { it.to == address && it.from != address }
                val outgoingEip721Transfers = eip721Transfers.filter { it.from == address }

                val eip1155Transfers =
                    decoration.eventInstances.mapNotNull { it as? Eip1155TransferEventInstance }
                val incomingEip1155Transfers =
                    eip1155Transfers.filter { it.to == address && it.from != address }
                val outgoingEip1155Transfers = eip1155Transfers.filter { it.from == address }

                val contractAddress = transaction.to
                val value = transaction.value

                when {
                    transaction.from == address && contractAddress != null && value != null -> {
                        EvmTransactionRecord(
                            transaction = transaction, token = baseToken, source = source,
                            contractAddress = contractAddress.eip55,
                            method = transaction.input?.let { evmLabelManager.methodLabel(it) },
                            incomingEvents = getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers) +
                                    getIncomingEip721Events(incomingEip721Transfers) +
                                    getIncomingEip1155Events(incomingEip1155Transfers),
                            outgoingEvents = getTransactionValueEvents(transaction) +
                                    getOutgoingEip20Events(outgoingEip20Transfers) +
                                    getOutgoingEip721Events(outgoingEip721Transfers) +
                                    getOutgoingEip1155Events(outgoingEip1155Transfers),
                            transactionRecordType = TransactionRecordType.EVM_CONTRACT_CALL
                        )
                    }

                    transaction.from != address && transaction.to != address -> {
                        EvmTransactionRecord(
                            transaction = transaction,
                            token = baseToken,
                            source = source,
                            spamManager = spamManager,
                            incomingEvents = getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers) +
                                    getIncomingEip721Events(incomingEip721Transfers) +
                                    getIncomingEip1155Events(incomingEip1155Transfers),
                            outgoingEvents = getOutgoingEip20Events(outgoingEip20Transfers) +
                                    getOutgoingEip721Events(outgoingEip721Transfers) +
                                    getOutgoingEip1155Events(outgoingEip1155Transfers),
                            transactionRecordType = TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL
                        )
                    }

                    else -> null
                }
            }

            else -> null
        }

        return transactionRecord ?: EvmTransactionRecord(
            transaction = transaction,
            token = baseToken,
            source = source,
            foreignTransaction = transaction.from != evmTransactionRepository.receiveAddress,
            transactionRecordType = TransactionRecordType.EVM
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

    private fun getEip20Value(
        tokenAddress: Address,
        amount: BigInteger,
        negative: Boolean,
        tokenInfo: TokenInfo? = null
    ): TransactionValue {
        val query = TokenQuery(evmTransactionRepository.getBlockchainType(), TokenType.Eip20(tokenAddress.hex))
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
                    coinIconPlaceholder = evmTransactionRepository.getBlockchainType().tokenIconPlaceholder
                )
            }

            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }

    private fun convertToTransactionValue(
        token: SwapDecoration.Token,
        amount: BigInteger,
        negative: Boolean
    ): TransactionValue {
        return when (token) {
            SwapDecoration.Token.EvmCoin -> {
                baseCoinValue(amount, negative)
            }

            is SwapDecoration.Token.Eip20Coin -> {
                getEip20Value(token.address, amount, negative, token.tokenInfo)
            }
        }
    }

    private fun convertToTransactionValue(
        token: OneInchDecoration.Token,
        amount: BigInteger,
        negative: Boolean
    ): TransactionValue {
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

    private fun convertToAmount(
        token: SwapDecoration.Token,
        amount: SwapDecoration.Amount,
        negative: Boolean
    ): EvmTransactionRecord.Amount {
        return when (amount) {
            is SwapDecoration.Amount.Exact -> EvmTransactionRecord.Amount.Exact(
                convertToTransactionValue(token, amount.value, negative)
            )

            is SwapDecoration.Amount.Extremum -> EvmTransactionRecord.Amount.Extremum(
                convertToTransactionValue(token, amount.value, negative)
            )
        }
    }

    private fun convertToAmount(
        token: OneInchDecoration.Token,
        amount: OneInchDecoration.Amount,
        negative: Boolean
    ): EvmTransactionRecord.Amount {
        return when (amount) {
            is OneInchDecoration.Amount.Exact -> EvmTransactionRecord.Amount.Exact(
                convertToTransactionValue(token, amount.value, negative)
            )

            is OneInchDecoration.Amount.Extremum -> EvmTransactionRecord.Amount.Extremum(
                convertToTransactionValue(token, amount.value, negative)
            )
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
                    getEip20Value(
                        transfer.contractAddress,
                        transfer.value,
                        false,
                        transfer.tokenInfo
                    )
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
                    getEip20Value(
                        transfer.contractAddress,
                        transfer.value,
                        true,
                        transfer.tokenInfo
                    )
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
                    nftUid = NftUid.Evm(
                        source.blockchain.type,
                        transfer.contractAddress.hex,
                        transfer.tokenId.toString()
                    ),
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
                    nftUid = NftUid.Evm(
                        source.blockchain.type,
                        transfer.contractAddress.hex,
                        transfer.tokenId.toString()
                    ),
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
                    nftUid = NftUid.Evm(
                        source.blockchain.type,
                        transfer.contractAddress.hex,
                        transfer.tokenId.toString()
                    ),
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
                    nftUid = NftUid.Evm(
                        source.blockchain.type,
                        transfer.contractAddress.hex,
                        transfer.tokenId.toString()
                    ),
                    value = convertAmount(transfer.value, 0, true),
                    tokenName = transfer.tokenInfo?.tokenName,
                    tokenSymbol = transfer.tokenInfo?.tokenSymbol
                )
            )
        }

}
