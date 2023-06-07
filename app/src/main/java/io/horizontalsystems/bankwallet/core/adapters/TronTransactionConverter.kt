package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.bankwallet.core.tokenIconPlaceholder
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronApproveTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.tron.TronTransactionRecord
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.decoration.NativeTransactionDecoration
import io.horizontalsystems.tronkit.decoration.TokenInfo
import io.horizontalsystems.tronkit.decoration.UnknownTransactionDecoration
import io.horizontalsystems.tronkit.decoration.trc20.ApproveTrc20Decoration
import io.horizontalsystems.tronkit.decoration.trc20.OutgoingTrc20Decoration
import io.horizontalsystems.tronkit.decoration.trc20.Trc20TransferEvent
import io.horizontalsystems.tronkit.hexStringToByteArrayOrNull
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.models.FullTransaction
import io.horizontalsystems.tronkit.models.InternalTransaction
import io.horizontalsystems.tronkit.models.TransferContract
import java.math.BigDecimal
import java.math.BigInteger

class TronTransactionConverter(
    private val coinManager: ICoinManager,
    private val tronKitWrapper: TronKitWrapper,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    private val tronKit: TronKit
        get() = tronKitWrapper.tronKit

    fun transactionRecord(fullTransaction: FullTransaction): TronTransactionRecord {
        val transaction = fullTransaction.transaction

        val transactionRecord = when (val decoration = fullTransaction.decoration) {
            is NativeTransactionDecoration -> {
                when (val contract = decoration.contract) {
                    is TransferContract -> {
                        if (contract.ownerAddress != tronKit.address) {
                            TronIncomingTransactionRecord(
                                transaction = transaction,
                                baseToken = baseToken,
                                source = source,
                                from = contract.ownerAddress.base58,
                                value = baseCoinValue(contract.amount, false),
                                spam = contract.amount < BigInteger.TEN
                            )
                        } else {
                            TronOutgoingTransactionRecord(
                                transaction = transaction,
                                baseToken = baseToken,
                                source = source,
                                to = contract.toAddress.base58,
                                value = baseCoinValue(contract.amount, true),
                                sentToSelf = contract.toAddress == tronKit.address
                            )
                        }
                    }

                    else -> null
                }
            }

            is OutgoingTrc20Decoration -> {
                TronOutgoingTransactionRecord(
                    transaction = transaction,
                    baseToken = baseToken,
                    source = source,
                    to = decoration.to.base58,
                    value = getEip20Value(decoration.contractAddress, decoration.value, true, decoration.tokenInfo),
                    sentToSelf = decoration.sentToSelf
                )
            }

            is ApproveTrc20Decoration -> {
                TronApproveTransactionRecord(
                    transaction,
                    baseToken,
                    source,
                    decoration.spender.base58,
                    getEip20Value(decoration.contractAddress, decoration.value, false)
                )
            }

            is UnknownTransactionDecoration -> {
                val address = tronKit.address

                val internalTransactions = decoration.internalTransactions.filter { it.to == address }

                val eip20Transfers = decoration.events.mapNotNull { it as? Trc20TransferEvent }
                val incomingEip20Transfers = eip20Transfers.filter { it.to == address && it.from != address }
                val outgoingEip20Transfers = eip20Transfers.filter { it.from == address }

                val contractAddress = decoration.toAddress

                when {
                    decoration.fromAddress == address && contractAddress != null -> {
                        TronContractCallTransactionRecord(
                            transaction, baseToken, source,
                            contractAddress.base58,
                            decoration.data?.hexStringToByteArrayOrNull()?.let { evmLabelManager.methodLabel(it) },
                            getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers),
                            getTransactionValueEvents(decoration) +
                                    getOutgoingEip20Events(outgoingEip20Transfers)
                        )
                    }

                    decoration.fromAddress != address && decoration.toAddress != address -> {
                        TronExternalContractCallTransactionRecord(
                            transaction, baseToken, source,
                            getInternalEvents(internalTransactions) +
                                    getIncomingEip20Events(incomingEip20Transfers),
                            getOutgoingEip20Events(outgoingEip20Transfers)
                        )
                    }

                    else -> null
                }
            }

            else -> null
        }

        return transactionRecord ?: TronTransactionRecord(
            transaction = transaction,
            baseToken = baseToken,
            source = source
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
        val query = TokenQuery(BlockchainType.Tron, TokenType.Eip20(tokenAddress.base58))
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
                    coinIconPlaceholder = BlockchainType.Tron.tokenIconPlaceholder
                )
            }

            else -> {
                TransactionValue.RawValue(value = amount)
            }
        }
    }


    private fun baseCoinValue(value: BigInteger, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, baseToken.decimals, negative)

        return TransactionValue.CoinValue(baseToken, amount)
    }


    private fun getInternalEvents(internalTransactions: List<InternalTransaction>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transaction in internalTransactions) {
            events.add(
                TransferEvent(transaction.from.base58, baseCoinValue(transaction.value, false))
            )
        }

        return events
    }

    private fun getTransactionValueEvents(decoration: UnknownTransactionDecoration): List<TransferEvent> {
        val value = decoration.value
        if (value == null || value <= BigInteger.ZERO) return listOf()

        return listOf(
            TransferEvent(decoration.toAddress?.base58, baseCoinValue(value, true))
        )
    }

    private fun getIncomingEip20Events(incomingTransfers: List<Trc20TransferEvent>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transfer in incomingTransfers) {
            events.add(
                TransferEvent(
                    transfer.from.base58,
                    getEip20Value(transfer.contractAddress, transfer.value, false, transfer.tokenInfo)
                )
            )
        }

        return events
    }

    private fun getOutgoingEip20Events(outgoingTransfers: List<Trc20TransferEvent>): List<TransferEvent> {
        val events: MutableList<TransferEvent> = mutableListOf()

        for (transfer in outgoingTransfers) {
            events.add(
                TransferEvent(
                    transfer.to.base58,
                    getEip20Value(transfer.contractAddress, transfer.value, true, transfer.tokenInfo)
                )
            )
        }

        return events
    }

}
