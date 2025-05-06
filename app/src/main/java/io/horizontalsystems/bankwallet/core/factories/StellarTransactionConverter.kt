package io.horizontalsystems.bankwallet.core.factories

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord
import io.horizontalsystems.bankwallet.core.adapters.StellarTransactionRecord.Type
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
                    )
                }
                incoming -> {
                    type = Type.Receive(
                        value = transactionValue,
                        from = payment.from,
                        comment = operation.memo,
                    )
                }
            }
        }

        operation.accountCreated?.let { accountCreated ->
            val transactionValue =
                TransactionValue.CoinValue(baseToken, accountCreated.startingBalance)

            type = Type.AccountCreated(
                funder = accountCreated.funder,
                account = accountCreated.account,
                value = transactionValue,
            )
        }

        operation.changeTrust?.let { changeTrust ->
            val token = getToken(changeTrust.asset)

            val transactionValue = if (token != null) {
                TransactionValue.CoinValue(token, changeTrust.limit)
            } else {
                val assetCode = changeTrust.asset.code
                TransactionValue.TokenValue(assetCode, assetCode, 7, changeTrust.limit)
            }

            type = Type.ChangeTrust(
                changeTrust.trustee,
                transactionValue
            )
        }

        return StellarTransactionRecord(baseToken, source, operation, type)
    }

    private fun getToken(asset: StellarAsset): Token? {
        val tokenType = when (asset) {
            StellarAsset.Native -> TokenType.Native
            is StellarAsset.Asset -> TokenType.Asset(asset.code, asset.issuer)
        }

        return coinManager.getToken(TokenQuery(BlockchainType.Stellar, tokenType))
    }


}
