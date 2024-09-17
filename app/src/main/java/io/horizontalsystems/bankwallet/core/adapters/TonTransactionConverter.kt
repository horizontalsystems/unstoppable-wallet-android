package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.managers.EvmLabelManager
import io.horizontalsystems.bankwallet.core.managers.TonKitWrapper
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.models.AccountAddress
import io.horizontalsystems.tonkit.models.Action
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.Jetton
import java.math.BigDecimal
import java.math.BigInteger

class TonTransactionConverter(
    private val coinManager: ICoinManager,
    private val tonKitWrapper: TonKitWrapper,
    private val source: TransactionSource,
    private val baseToken: Token,
    private val evmLabelManager: EvmLabelManager
) {
    val address = tonKitWrapper.tonKit.receiveAddress

    fun createTransactionRecord(event: Event): TonTransactionRecord {
        val actions = event.actions.map { action ->
            val status = when (action.status) {
                Action.Status.OK -> TransactionStatus.Completed
                Action.Status.FAILED -> TransactionStatus.Failed
            }

            TonTransactionRecord.Action(
                type = getActionType(action),
                status = status
            )
        }

        return TonTransactionRecord(source, event, baseToken, actions)
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

    private fun tonValue(value: BigInteger, negative: Boolean): TransactionValue {
        val amount = convertAmount(value, baseToken.decimals, negative)
        return TransactionValue.CoinValue(baseToken, amount)
    }

    private fun jettonValue(jetton: Jetton, value: BigInteger, negative: Boolean): TransactionValue {
        val query = TokenQuery(BlockchainType.Ton, TokenType.Jetton(jetton.address.toUserFriendly(bounceable = true)))

        val token = coinManager.getToken(query)

        return if (token != null) {
            TransactionValue.CoinValue(token, convertAmount(value, token.decimals, negative))
        } else {
            TransactionValue.JettonValue(
                jetton.name,
                jetton.symbol,
                jetton.decimals,
                convertAmount(value, jetton.decimals, negative)
            )
        }
    }

    private fun format(address: AccountAddress): String {
        return address.address.toUserFriendly(bounceable = !address.isWallet)
    }

    private fun getActionType(action: Action): TonTransactionRecord.Action.Type {
        action.tonTransfer?.let { tonTransfer ->
            return when {
                tonTransfer.sender.address == address -> {
                    TonTransactionRecord.Action.Type.Send(
                        value = tonValue(tonTransfer.amount, true),
                        to = format(tonTransfer.recipient),
                        sentToSelf = tonTransfer.recipient.address == address,
                        comment = tonTransfer.comment
                    )
                }

                tonTransfer.recipient.address == address -> {
                    TonTransactionRecord.Action.Type.Receive(
                        value = tonValue(tonTransfer.amount, false),
                        from = format(tonTransfer.sender),
                        comment = tonTransfer.comment
                    )
                }

                else -> TonTransactionRecord.Action.Type.Unsupported("Ton Transfer")
            }
        }

        action.jettonTransfer?.let { jettonTransfer ->
            val recipient = jettonTransfer.recipient
            val sender = jettonTransfer.sender

            return when {
                jettonTransfer.sender?.address == address && recipient != null -> {
                    TonTransactionRecord.Action.Type.Send(
                        value = jettonValue(jettonTransfer.jetton, jettonTransfer.amount, true),
                        to = format(recipient),
                        sentToSelf = jettonTransfer.recipient?.address == address,
                        comment = jettonTransfer.comment
                    )
                }

                jettonTransfer.recipient?.address == address && sender != null -> {
                    TonTransactionRecord.Action.Type.Receive(
                        value = jettonValue(jettonTransfer.jetton, jettonTransfer.amount, false),
                        from = format(sender),
                        comment = jettonTransfer.comment
                    )
                }

                else -> TonTransactionRecord.Action.Type.Unsupported("Jetton Transfer")
            }
        }

        return TonTransactionRecord.Action.Type.Unsupported(action.type.name)
    }
}
