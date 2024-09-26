package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.tonkit.Address
import io.horizontalsystems.tonkit.models.AccountAddress
import io.horizontalsystems.tonkit.models.Action
import io.horizontalsystems.tonkit.models.Event
import io.horizontalsystems.tonkit.models.Jetton
import java.math.BigDecimal
import java.math.BigInteger

class TonTransactionConverter(
    private val address: Address,
    private val coinManager: ICoinManager,
    private val source: TransactionSource,
    private val baseToken: Token
) {

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
                convertAmount(value, jetton.decimals, negative),
                jetton.image
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

        action.jettonBurn?.let { jettonBurn ->
            return TonTransactionRecord.Action.Type.Burn(
                value = jettonValue(jettonBurn.jetton, jettonBurn.amount, true)
            )
        }

        action.jettonMint?.let { jettonMint ->
            return TonTransactionRecord.Action.Type.Mint(
                value = jettonValue(jettonMint.jetton, jettonMint.amount, false)
            )
        }

        action.contractDeploy?.let { contractDeploy ->
            return TonTransactionRecord.Action.Type.ContractDeploy(
                interfaces = contractDeploy.interfaces
            )
        }

        action.jettonSwap?.let { jettonSwap ->
            return TonTransactionRecord.Action.Type.Swap(
                routerName = jettonSwap.router.name,
                routerAddress = format(jettonSwap.router),
                valueIn = jettonSwap.jettonMasterIn?.let {
                    jettonValue(it, jettonSwap.amountIn, true)
                } ?: tonValue(jettonSwap.tonIn ?: BigInteger.ZERO, true),
                valueOut = jettonSwap.jettonMasterOut?.let {
                    jettonValue(it, jettonSwap.amountOut, false)
                } ?: tonValue(jettonSwap.tonOut ?: BigInteger.ZERO, false),
            )
        }

        action.smartContractExec?.let { smartContractExec ->
            return TonTransactionRecord.Action.Type.ContractCall(
                address = format(smartContractExec.contract),
                value = tonValue(smartContractExec.tonAttached, true),
                operation = smartContractExec.operation
            )
        }

        return TonTransactionRecord.Action.Type.Unsupported(action.type.name)
    }
}
