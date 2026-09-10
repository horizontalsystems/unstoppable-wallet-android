package io.horizontalsystems.walletkit.modules.walletconnect.stellar

import io.horizontalsystems.walletkit.R
import io.horizontalsystems.walletkit.core.providers.Translator
import io.horizontalsystems.walletkit.modules.sendevmtransaction.SectionViewItem
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ValueType
import io.horizontalsystems.walletkit.modules.sendevmtransaction.ViewItem
import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCStellarTxSummary.AssetInfo
import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCStellarTxSummary.Op
import io.horizontalsystems.walletkit.modules.walletconnect.stellar.WCStellarTxSummary.formatAmount
import org.stellar.sdk.Transaction
import java.math.BigDecimal

object WCStellarHelper {

    /**
     * Sign-screen rows for [transaction]: a warning banner first when the user would be signing
     * something they cannot see (an operation this decoder does not understand, or an account
     * control change), then a section per operation with its destination / amount / asset, then
     * the memo and the raw XDR. [walletAddress] is the active account; a transaction sourced from
     * some other account is flagged.
     */
    fun getTransactionViewItems(transaction: Transaction, xdr: String, walletAddress: String?): List<SectionViewItem> {
        val decoded = WCStellarTxSummary.decode(transaction)
        val sections = mutableListOf<SectionViewItem>()

        warningSection(decoded)?.let { sections.add(it) }

        if (walletAddress != null && decoded.sourceAccount != walletAddress) {
            sections.add(
                SectionViewItem(
                    listOf(
                        ViewItem.Alert(
                            title = Translator.getString(R.string.WalletConnect_Stellar_SourceAccount),
                            text = Translator.getString(R.string.WalletConnect_Stellar_ForeignSource),
                            critical = true
                        ),
                        ViewItem.Address(Translator.getString(R.string.WalletConnect_Stellar_SourceAccount), decoded.sourceAccount),
                    )
                )
            )
        }

        decoded.operations.forEach { op ->
            sections.add(SectionViewItem(operationRows(op, walletAddress)))
        }

        sections.add(
            SectionViewItem(
                buildList {
                    decoded.memo?.let {
                        add(ViewItem.Value(Translator.getString(R.string.TransactionInfo_Memo), it, ValueType.Regular))
                    }
                    add(ViewItem.Input("Transaction XDR", xdr))
                }
            )
        )

        return sections
    }

    private fun warningSection(decoded: WCStellarTxSummary.Decoded): SectionViewItem? {
        val setOptions = decoded.operations.any { it is Op.SetOptions }
        val unknown = decoded.operations.filterIsInstance<Op.Unknown>()

        val alert = when {
            setOptions -> ViewItem.Alert(
                title = Translator.getString(R.string.WalletConnect_Stellar_SetOptions_Title),
                text = Translator.getString(R.string.WalletConnect_Stellar_SetOptions),
                critical = true
            )

            unknown.isNotEmpty() -> ViewItem.Alert(
                title = Translator.getString(R.string.WalletConnect_Stellar_UnreadableOperation_Title),
                text = Translator.getString(
                    R.string.WalletConnect_Stellar_UnreadableOperation,
                    unknown.joinToString(", ") { it.typeName }
                ),
                critical = false
            )

            else -> return null
        }
        return SectionViewItem(listOf(alert))
    }

    private fun operationRows(op: Op, walletAddress: String?): List<ViewItem> = buildList {
        fun value(titleRes: Int, value: String, type: ValueType = ValueType.Regular) =
            add(ViewItem.Value(Translator.getString(titleRes), value, type))

        fun to(address: String) = add(ViewItem.Address(Translator.getString(R.string.Send_Confirmation_To), address))

        fun amount(titleRes: Int, amount: BigDecimal, asset: AssetInfo, type: ValueType) {
            value(titleRes, formatAmount(amount, asset), type)
            asset.issuer?.let { add(ViewItem.Address(Translator.getString(R.string.WalletConnect_Stellar_AssetIssuer), it)) }
        }

        fun operation(nameRes: Int) = value(R.string.WalletConnect_Stellar_Operation, Translator.getString(nameRes))

        when (op) {
            is Op.Payment -> {
                operation(R.string.WalletConnect_Stellar_Op_Payment)
                amount(R.string.WalletConnect_Stellar_Amount, op.amount, op.asset, ValueType.Outgoing)
                to(op.destination)
            }

            is Op.CreateAccount -> {
                operation(R.string.WalletConnect_Stellar_Op_CreateAccount)
                amount(R.string.WalletConnect_Stellar_Amount, op.startingBalance, AssetInfo("XLM", null), ValueType.Outgoing)
                to(op.destination)
            }

            is Op.AccountMerge -> {
                operation(R.string.WalletConnect_Stellar_Op_AccountMerge)
                to(op.destination)
            }

            is Op.PathPaymentStrictSend -> {
                operation(R.string.WalletConnect_Stellar_Op_PathPayment)
                amount(R.string.WalletConnect_Stellar_Amount, op.sendAmount, op.sendAsset, ValueType.Outgoing)
                amount(R.string.WalletConnect_Stellar_MinReceived, op.destMin, op.destAsset, ValueType.Incoming)
                to(op.destination)
            }

            is Op.PathPaymentStrictReceive -> {
                operation(R.string.WalletConnect_Stellar_Op_PathPayment)
                amount(R.string.WalletConnect_Stellar_MaxSent, op.sendMax, op.sendAsset, ValueType.Outgoing)
                amount(R.string.WalletConnect_Stellar_Received, op.destAmount, op.destAsset, ValueType.Incoming)
                to(op.destination)
            }

            is Op.ChangeTrust -> {
                operation(R.string.WalletConnect_Stellar_Op_ChangeTrust)
                op.asset?.let { asset ->
                    value(R.string.WalletConnect_Stellar_Asset, asset.code)
                    asset.issuer?.let { add(ViewItem.Address(Translator.getString(R.string.WalletConnect_Stellar_AssetIssuer), it)) }
                }
                value(R.string.WalletConnect_Stellar_TrustLimit, op.limit.stripTrailingZeros().toPlainString())
            }

            is Op.ManageOffer -> {
                operation(if (op.isBuy) R.string.WalletConnect_Stellar_Op_BuyOffer else R.string.WalletConnect_Stellar_Op_SellOffer)
                amount(R.string.WalletConnect_Stellar_Amount, op.amount, if (op.isBuy) op.buying else op.selling, ValueType.Regular)
                value(R.string.WalletConnect_Stellar_Selling, op.selling.code)
                value(R.string.WalletConnect_Stellar_Buying, op.buying.code)
                value(R.string.WalletConnect_Stellar_Price, op.price.stripTrailingZeros().toPlainString())
            }

            is Op.SetOptions -> {
                operation(R.string.WalletConnect_Stellar_Op_SetOptions)
                // A signer being added (weight > 0) is the account-takeover shape; show it red.
                op.signer?.let { signer ->
                    add(ViewItem.Value(Translator.getString(R.string.WalletConnect_Stellar_Signer), signer, ValueType.Warning))
                }
                op.signerWeight?.let { value(R.string.WalletConnect_Stellar_SignerWeight, it.toString(), ValueType.Warning) }
                op.masterKeyWeight?.let { value(R.string.WalletConnect_Stellar_MasterKeyWeight, it.toString(), ValueType.Warning) }
                op.lowThreshold?.let { value(R.string.WalletConnect_Stellar_LowThreshold, it.toString(), ValueType.Warning) }
                op.mediumThreshold?.let { value(R.string.WalletConnect_Stellar_MediumThreshold, it.toString(), ValueType.Warning) }
                op.highThreshold?.let { value(R.string.WalletConnect_Stellar_HighThreshold, it.toString(), ValueType.Warning) }
                op.setFlags?.let { value(R.string.WalletConnect_Stellar_SetFlags, it.toString()) }
                op.clearFlags?.let { value(R.string.WalletConnect_Stellar_ClearFlags, it.toString()) }
                op.homeDomain?.let { value(R.string.WalletConnect_Stellar_HomeDomain, it) }
                op.inflationDestination?.let { add(ViewItem.Address(Translator.getString(R.string.WalletConnect_Stellar_InflationDestination), it)) }
            }

            is Op.Unknown -> value(R.string.WalletConnect_Stellar_Operation, op.typeName, ValueType.Warning)
        }

        // An operation sourced from an account other than the wallet is paid by that account.
        op.sourceAccount?.takeIf { walletAddress != null && it != walletAddress }?.let {
            add(ViewItem.Address(Translator.getString(R.string.WalletConnect_Stellar_SourceAccount), it))
        }
    }
}
