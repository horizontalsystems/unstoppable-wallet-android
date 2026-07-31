package io.horizontalsystems.walletkit.core.factories

import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.adapters.StellarTransactionRecord
import io.horizontalsystems.walletkit.core.adapters.StellarTransactionRecord.Type
import io.horizontalsystems.walletkit.core.tokenIconPlaceholder
import io.horizontalsystems.walletkit.entities.TransactionValue
import io.horizontalsystems.walletkit.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.Token
import io.horizontalsystems.marketkit.models.TokenQuery
import io.horizontalsystems.marketkit.models.TokenType
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.StellarAsset

class StellarTransactionConverter(
    private val source: TransactionSource,
    private val selfAddress: String,
    private val coinManager: ICoinManager,
    private val baseToken: Token,
) {
    suspend fun convert(operation: Operation): StellarTransactionRecord {
        var type: Type = Type.Unsupported(operation.type)

        operation.payment?.let { payment ->
            val outgoing = payment.from == selfAddress
            val incoming = payment.to == selfAddress

            val token = getToken(payment.asset)
            var amount = payment.amount
            if (outgoing) {
                amount = amount.negate()
            }

            val transactionValue = if (token != null) {
                TransactionValue.CoinValue(token, amount)
            } else {
                TransactionValue.RawValue(amount.unscaledValue())
            }

            when {
                outgoing -> {
                    type = Type.Send(
                        value = transactionValue,
                        to = payment.to,
                        sentToSelf = incoming,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }

                incoming -> {
                    type = Type.Receive(
                        value = transactionValue,
                        from = payment.from,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }
            }
        }

        operation.accountCreated?.let { accountCreated ->
            val outgoing = accountCreated.funder == selfAddress
            val incoming = accountCreated.account == selfAddress

            var amount = accountCreated.startingBalance
            if (outgoing) {
                amount = amount.negate()
            }

            val transactionValue = TransactionValue.CoinValue(baseToken, amount)

            when {
                outgoing -> {
                    type = Type.Send(
                        value = transactionValue,
                        to = accountCreated.account,
                        sentToSelf = incoming,
                        comment = operation.memo,
                        accountCreated = true
                    )
                }

                incoming -> {
                    type = Type.Receive(
                        value = transactionValue,
                        from = accountCreated.funder,
                        comment = operation.memo,
                        accountCreated = true
                    )
                }
            }
        }

        operation.pathPayment?.let { pathPayment ->
            val outgoing = pathPayment.from == selfAddress
            val incoming = pathPayment.to == selfAddress

            when {
                // A path payment to self spends sourceAsset and delivers asset — a swap
                // (the Stellar DEX route of the swap providers).
                outgoing && incoming -> {
                    type = Type.Swap(
                        valueIn = transactionValue(pathPayment.sourceAsset, pathPayment.sourceAmount.negate()),
                        valueOut = transactionValue(pathPayment.asset, pathPayment.amount),
                    )
                }

                outgoing -> {
                    type = Type.Send(
                        value = transactionValue(pathPayment.sourceAsset, pathPayment.sourceAmount.negate()),
                        to = pathPayment.to,
                        sentToSelf = false,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }

                incoming -> {
                    type = Type.Receive(
                        value = transactionValue(pathPayment.asset, pathPayment.amount),
                        from = pathPayment.from,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }
            }
        }

        operation.contractBalanceChanges?.let { changes ->
            // Horizon's per-asset balance movements of a contract call (SAC events),
            // aggregated by asset. Sold one asset and received another — a swap
            // (Soroswap/Aquarius); one-directional movements render as plain transfers
            // (e.g. the Axelar ITS deposit); multi-asset movements stay Unsupported
            // rather than showing understated amounts.
            when (val movement = StellarContractMovement.resolve(changes, selfAddress)) {
                is StellarContractMovement.Swap -> {
                    type = Type.Swap(
                        valueIn = transactionValue(movement.sold.asset, movement.sold.amount),
                        valueOut = transactionValue(movement.received.asset, movement.received.amount),
                    )
                }

                is StellarContractMovement.Outgoing -> {
                    type = Type.Send(
                        value = transactionValue(movement.movement.asset, movement.movement.amount),
                        to = movement.counterparty ?: "",
                        sentToSelf = false,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }

                is StellarContractMovement.Incoming -> {
                    type = Type.Receive(
                        value = transactionValue(movement.movement.asset, movement.movement.amount),
                        from = movement.counterparty ?: "",
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }

                StellarContractMovement.None,
                StellarContractMovement.Unrepresentable -> Unit
            }
        }

        operation.changeTrust?.let { changeTrust ->
            val token = getToken(changeTrust.asset)

            val transactionValue = if (token != null) {
                TransactionValue.CoinValue(token, changeTrust.limit)
            } else {
                val assetCode = changeTrust.asset.code
                TransactionValue.TokenValue(
                    tokenName = assetCode,
                    tokenCode = assetCode,
                    tokenDecimals = 7,
                    value = changeTrust.limit,
                    coinIconPlaceholder = BlockchainType.Stellar.tokenIconPlaceholder
                )
            }

            type = Type.ChangeTrust(
                changeTrust.trustee,
                transactionValue
            )
        }

        val eventsForPhishingCheck = StellarTransactionRecord.eventsForPhishingCheck(type)
        val txHash = operation.transactionHash.hexStringToByteArrayOrNull()
        val spam = if (txHash != null) {
            App.spamManager.isSpam(
                txHash,
                eventsForPhishingCheck,
                source,
                operation.timestamp,
                null,
                operation.id
            )
        } else {
            false
        }

        return StellarTransactionRecord(baseToken, source, operation, type, spam)
    }

    private fun transactionValue(asset: StellarAsset, amount: java.math.BigDecimal): TransactionValue {
        val token = getToken(asset)
        return if (token != null) {
            TransactionValue.CoinValue(token, amount)
        } else {
            val assetCode = when (asset) {
                StellarAsset.Native -> "XLM"
                is StellarAsset.Asset -> asset.code
            }
            TransactionValue.TokenValue(
                tokenName = assetCode,
                tokenCode = assetCode,
                tokenDecimals = 7,
                value = amount,
                coinIconPlaceholder = BlockchainType.Stellar.tokenIconPlaceholder
            )
        }
    }

    private fun getToken(asset: StellarAsset): Token? {
        val tokenType = when (asset) {
            StellarAsset.Native -> TokenType.Native
            is StellarAsset.Asset -> TokenType.Asset(asset.code, asset.issuer)
        }

        return coinManager.getToken(TokenQuery(BlockchainType.Stellar, tokenType))
    }


}
