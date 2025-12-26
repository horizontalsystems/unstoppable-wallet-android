package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord.Type
import io.horizontalsystems.bankwallet.core.managers.SpamManager
import io.horizontalsystems.bankwallet.core.tokenIconPlaceholder
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
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
    fun convert(operation: Operation): StellarTransactionRecord {
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
        val spam = SpamManager.isSpam(eventsForPhishingCheck)

        return StellarTransactionRecord(baseToken, source, operation, type, spam)
    }

    private fun getToken(asset: StellarAsset): Token? {
        val tokenType = when (asset) {
            StellarAsset.Native -> TokenType.Native
            is StellarAsset.Asset -> TokenType.Asset(asset.code, asset.issuer)
        }

        return coinManager.getToken(TokenQuery(BlockchainType.Stellar, tokenType))
    }


}
