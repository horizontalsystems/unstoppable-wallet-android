package cash.p.terminal.core.factories

import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.tokenIconPlaceholder
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.stellar.StellarTransactionRecord
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.entities.TokenQuery
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.stellarkit.room.Operation
import io.horizontalsystems.stellarkit.room.StellarAsset
import kotlin.let

class StellarTransactionConverter(
    private val source: TransactionSource,
    private val selfAddress: String,
    private val coinManager: ICoinManager,
    private val baseToken: Token,
) {
    fun convert(operation: Operation): StellarTransactionRecord {
        var type: StellarTransactionRecord.Type = StellarTransactionRecord.Type.Unsupported(operation.type)

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
                    type = StellarTransactionRecord.Type.Send(
                        value = transactionValue,
                        to = payment.to,
                        sentToSelf = incoming,
                        comment = operation.memo,
                        accountCreated = false,
                    )
                }
                incoming -> {
                    type = StellarTransactionRecord.Type.Receive(
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
                    type = StellarTransactionRecord.Type.Send(
                        value = transactionValue,
                        to = accountCreated.account,
                        sentToSelf = incoming,
                        comment = operation.memo,
                        accountCreated = true
                    )
                }
                incoming -> {
                    type = StellarTransactionRecord.Type.Receive(
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

            type = StellarTransactionRecord.Type.ChangeTrust(
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
